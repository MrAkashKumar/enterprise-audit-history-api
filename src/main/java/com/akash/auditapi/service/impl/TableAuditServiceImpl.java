package com.akash.auditapi.service.impl;

import com.akash.auditapi.dao.AuditIdPage;
import com.akash.auditapi.dao.ApprovalDao;
import com.akash.auditapi.dao.TableAuditDao;
import com.akash.auditapi.dto.TableDescriptor;
import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.response.ApprovalResponse;
import com.akash.auditapi.dto.response.AuditedRowResponse;
import com.akash.auditapi.dto.response.SearchResponse;
import com.akash.auditapi.exception.AuditApiException;
import com.akash.auditapi.resolver.AuditableTableCatalog;
import com.akash.auditapi.resolver.ApprovalTableResolver;
import com.akash.auditapi.resolver.TableDescriptorResolver;
import com.akash.auditapi.service.AuditHistoryAssembler;
import com.akash.auditapi.service.TableAuditService;
import com.akash.auditapi.validation.PaginationValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.akash.auditapi.exception.ApiMessages.AUDIT_SEARCH_COMPLETED_LOG;
import static com.akash.auditapi.exception.ApiMessages.AUDIT_SEARCH_STARTED_LOG;
import static com.akash.auditapi.exception.ApiMessages.AUDIT_TABLE_LIST_LOG;
import static com.akash.auditapi.exception.ApiMessages.APPROVAL_ENRICHMENT_SKIPPED_LOG;
import static com.akash.auditapi.exception.ApiMessages.NULL_ENTITY_ID;
import static com.akash.auditapi.exception.ApiMessages.duplicateApprovalRecord;

/**
 * Orchestrates validation, table resolution, JDBC reads, and audit response assembly.
 * It is the transactional implementation behind the generic audit controller.
 */
@Service
@Slf4j
public class TableAuditServiceImpl implements TableAuditService {
    private final AuditableTableCatalog tableCatalog;
    private final TableDescriptorResolver tableResolver;
    private final TableAuditDao auditDao;
    private final ApprovalDao approvalDao;
    private final ApprovalTableResolver approvalTableResolver;
    private final PaginationValidator paginationValidator;
    private final AuditHistoryAssembler historyAssembler;

    public TableAuditServiceImpl(AuditableTableCatalog tableCatalog,
                                 TableDescriptorResolver tableResolver,
                                 TableAuditDao auditDao,
                                 ApprovalDao approvalDao,
                                 ApprovalTableResolver approvalTableResolver,
                                 PaginationValidator paginationValidator,
                                 AuditHistoryAssembler historyAssembler) {
        this.tableCatalog = tableCatalog;
        this.tableResolver = tableResolver;
        this.auditDao = auditDao;
        this.approvalDao = approvalDao;
        this.approvalTableResolver = approvalTableResolver;
        this.paginationValidator = paginationValidator;
        this.historyAssembler = historyAssembler;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllTableLabels() {
        List<String> labels = tableCatalog.labels();
        log.info(AUDIT_TABLE_LIST_LOG, labels.size());
        return labels;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse sourceWithAuditHistory(String tableName, int pageNo, int pageSize) {
        paginationValidator.validate(pageNo, pageSize);
        String sourceTable = tableCatalog.resolve(tableName);
        log.info(AUDIT_SEARCH_STARTED_LOG, sourceTable, pageNo, pageSize);
        TableDescriptor table = tableResolver.resolve(sourceTable);
        AuditIdPage idPage = auditDao.findIdPage(table, pageNo, pageSize);
        long total = idPage.totalElements();
        List<Object> ids = idPage.ids();
        List<AuditedRowResponse> rows = loadRows(table, ids);
        long totalPages = total == 0 ? 0 : Math.ceilDiv(total, pageSize);
        SearchResponse response = new SearchResponse(pageNo, pageSize, rows.size(), total,
                totalPages, pageNo > 0,
                (long) pageNo + 1 < totalPages, rows);
        log.info(AUDIT_SEARCH_COMPLETED_LOG, sourceTable, rows.size(), total);
        return response;
    }

    private List<AuditedRowResponse> loadRows(TableDescriptor table, List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> auditRows = table.hasAuditHistory()
                ? auditDao.findAuditRows(table, ids)
                : List.of();
        List<AuditedRowResponse> rows = historyAssembler.assemble(table, ids,
                auditDao.findSourceRows(table, ids), auditRows);
        return enrichWithApproval(table.sourceTable(), ids, rows);
    }

    private List<AuditedRowResponse> enrichWithApproval(String sourceTable, List<Object> ids,
                                                         List<AuditedRowResponse> rows) {
        try {
            List<ApprovalRecord> approvalRows = approvalTableResolver.resolve(sourceTable)
                    .map(table -> approvalDao.findByIds(table, ids))
                    .orElseGet(List::of);
            return mergeApproval(rows, approvalRows);
        } catch (DataAccessException | AuditApiException | IllegalStateException exception) {
            log.warn(APPROVAL_ENRICHMENT_SKIPPED_LOG, sourceTable,
                    exception.getClass().getSimpleName());
            return rows;
        }
    }

    private List<AuditedRowResponse> mergeApproval(List<AuditedRowResponse> rows,
                                                    List<ApprovalRecord> approvalRecords) {
        if (approvalRecords.isEmpty()) {
            return rows;
        }
        Map<String, ApprovalResponse> approvalById = new HashMap<>();
        for (ApprovalRecord record : approvalRecords) {
            ApprovalResponse previous = approvalById.putIfAbsent(approvalKey(record.id()),
                    ApprovalResponse.present(record.makerUsername(), record.checkerUsername()));
            if (previous != null) {
                throw new IllegalStateException(duplicateApprovalRecord(record.id()));
            }
        }
        return rows.stream().map(row -> copyWithApproval(row,
                approvalById.getOrDefault(approvalKey(row.id()), ApprovalResponse.ABSENT))).toList();
    }

    private AuditedRowResponse copyWithApproval(AuditedRowResponse row, ApprovalResponse approval) {
        return new AuditedRowResponse(row.id(), row.originalRecordPresent(), row.originalData(),
                approval, row.changeSummary(), row.auditHistory());
    }

    private String approvalKey(Object id) {
        if (id == null) {
            throw new IllegalStateException(NULL_ENTITY_ID);
        }
        if (id instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        }
        return id.toString();
    }
}
