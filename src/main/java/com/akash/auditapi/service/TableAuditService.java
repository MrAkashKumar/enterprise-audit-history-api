package com.akash.auditapi.service;

import com.akash.auditapi.dao.AuditIdPage;
import com.akash.auditapi.dao.TableAuditDao;
import com.akash.auditapi.model.AuditableTable;
import com.akash.auditapi.model.AuditedRowResponse;
import com.akash.auditapi.model.SearchResponse;
import com.akash.auditapi.model.TableDescriptor;
import com.akash.auditapi.resolver.TableDescriptorResolver;
import com.akash.auditapi.validation.PaginationValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableAuditService {
    private final TableDescriptorResolver tableResolver;
    private final TableAuditDao auditDao;
    private final PaginationValidator paginationValidator;
    private final AuditHistoryAssembler historyAssembler;

    public TableAuditService(TableDescriptorResolver tableResolver, TableAuditDao auditDao,
                             PaginationValidator paginationValidator,
                             AuditHistoryAssembler historyAssembler) {
        this.tableResolver = tableResolver;
        this.auditDao = auditDao;
        this.paginationValidator = paginationValidator;
        this.historyAssembler = historyAssembler;
    }

    @Transactional(readOnly = true)
    public SearchResponse sourceWithAuditHistory(String tableName, int pageNo, int pageSize) {
        paginationValidator.validate(pageNo, pageSize);
        TableDescriptor table = tableResolver.resolve(AuditableTable.resolveTableName(tableName));
        AuditIdPage idPage = auditDao.findIdPage(table, pageNo, pageSize);
        long total = idPage.totalElements();
        List<Object> ids = idPage.ids();
        List<AuditedRowResponse> rows = loadRows(table, ids);
        long totalPages = total == 0 ? 0 : Math.ceilDiv(total, pageSize);
        return new SearchResponse(pageNo, pageSize, rows.size(), total, totalPages, pageNo > 0,
                (long) pageNo + 1 < totalPages, rows);
    }

    private List<AuditedRowResponse> loadRows(TableDescriptor table, List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return historyAssembler.assemble(table, ids,
                auditDao.findSourceRows(table, ids), auditDao.findAuditRows(table, ids));
    }
}
