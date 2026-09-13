package com.akash.auditapi.service;

import com.akash.auditapi.dao.AuditIdPage;
import com.akash.auditapi.dao.TableAuditDao;
import com.akash.auditapi.model.AuditRevisionResponse;
import com.akash.auditapi.model.AuditableTable;
import com.akash.auditapi.model.AuditedRowResponse;
import com.akash.auditapi.model.ChangeSummary;
import com.akash.auditapi.model.RevisionOperation;
import com.akash.auditapi.model.SearchResponse;
import com.akash.auditapi.model.TableDescriptor;
import com.akash.auditapi.resolver.RevisionOperationResolver;
import com.akash.auditapi.resolver.TableDescriptorResolver;
import com.akash.auditapi.validation.PaginationValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.akash.auditapi.exception.ApiMessages.NULL_ENTITY_ID;

@Service
public class TableAuditService {
    private final TableDescriptorResolver tableResolver;
    private final TableAuditDao auditDao;
    private final PaginationValidator paginationValidator;
    private final RevisionOperationResolver operationResolver;

    public TableAuditService(TableDescriptorResolver tableResolver, TableAuditDao auditDao,
                             PaginationValidator paginationValidator,
                             RevisionOperationResolver operationResolver) {
        this.tableResolver = tableResolver;
        this.auditDao = auditDao;
        this.paginationValidator = paginationValidator;
        this.operationResolver = operationResolver;
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
        return new SearchResponse(table.sourceTable(), table.auditTable(), pageNo, pageSize,
                rows.size(), total, totalPages, pageNo > 0,
                (long) pageNo + 1 < totalPages, rows);
    }

    private List<AuditedRowResponse> loadRows(TableDescriptor table, List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<String, Map<String, Object>> sourceById = new HashMap<>();
        for (Map<String, Object> row : auditDao.findSourceRows(table, ids)) {
            sourceById.put(key(row.get(table.idColumn())), row);
        }
        Map<String, HistoryAccumulator> historyById = new HashMap<>();
        for (Map<String, Object> row : auditDao.findAuditRows(table, ids)) {
            String entityKey = key(row.get(table.idColumn()));
            HistoryAccumulator history = historyById.computeIfAbsent(
                    entityKey, ignored -> new HistoryAccumulator());
            RevisionOperation operation = operationResolver.resolve(row.get(table.revisionTypeColumn()));
            AuditRevisionResponse revision = new AuditRevisionResponse(
                    history.size() + 1, row.get(table.auditOrderColumn()),
                    operation.code(), operation, row);
            history.add(revision);
        }

        List<AuditedRowResponse> rows = new ArrayList<>(ids.size());
        for (Object id : ids) {
            String entityKey = key(id);
            Map<String, Object> originalData = sourceById.get(entityKey);
            HistoryAccumulator history = historyById.get(entityKey);
            rows.add(new AuditedRowResponse(id, originalData != null, originalData,
                    history == null ? ChangeSummary.EMPTY : history.summary(),
                    history == null ? List.of() : history.revisions()));
        }
        return rows;
    }

    private String key(Object id) {
        if (id == null) {
            throw new IllegalStateException(NULL_ENTITY_ID);
        }
        return id.toString();
    }

    private static final class HistoryAccumulator {
        private final List<AuditRevisionResponse> revisions = new ArrayList<>();
        private long inserts;
        private long updates;
        private long deletes;
        private long unknown;

        void add(AuditRevisionResponse revision) {
            revisions.add(revision);
            switch (revision.operation()) {
                case INSERT -> inserts++;
                case UPDATE -> updates++;
                case DELETE -> deletes++;
                case UNKNOWN -> unknown++;
            }
        }

        int size() {
            return revisions.size();
        }

        List<AuditRevisionResponse> revisions() {
            return revisions;
        }

        ChangeSummary summary() {
            return new ChangeSummary(revisions.size(), inserts, updates, deletes, unknown,
                    revisions.getFirst().revision(), revisions.getLast().revision());
        }
    }
}
