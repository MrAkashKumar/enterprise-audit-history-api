package com.akash.auditapi.service;

import com.akash.auditapi.dao.TableAuditDao;
import com.akash.auditapi.model.AuditRevisionResponse;
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
import java.util.LinkedHashMap;
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
        TableDescriptor table = tableResolver.resolve(tableName);
        long total = auditDao.countDistinctIds(table);
        List<Object> ids = auditDao.findPageIds(table, pageNo, pageSize);
        Map<String, Object> responseIds = new LinkedHashMap<>();
        ids.forEach(id -> responseIds.put(key(id), id));
        Map<String, Map<String, Object>> sourceById = new LinkedHashMap<>();
        for (Map<String, Object> row : auditDao.findSourceRows(table, ids))
            sourceById.put(key(row.get(table.idColumn())), row);
        Map<String, List<AuditRevisionResponse>> historyById = new LinkedHashMap<>();
        for (Map<String, Object> row : auditDao.findAuditRows(table, ids)) {
            String entityKey = key(row.get(table.idColumn()));
            List<AuditRevisionResponse> revisions = historyById.computeIfAbsent(
                    entityKey, ignored -> new ArrayList<>());
            RevisionOperation operation = operationResolver.resolve(row.get(table.revisionTypeColumn()));
            AuditRevisionResponse revision = new AuditRevisionResponse(
                    revisions.size() + 1, row.get(table.auditOrderColumn()),
                    operation.code(), operation, row);
            revisions.add(revision);
        }
        List<AuditedRowResponse> rows = responseIds.entrySet().stream()
                .map(e -> {
                    List<AuditRevisionResponse> history = historyById.getOrDefault(e.getKey(), List.of());
                    Map<String, Object> originalData = sourceById.get(e.getKey());
                    return new AuditedRowResponse(e.getValue(), originalData != null, originalData,
                            summarize(history), history);
                }).toList();
        long totalPages = total == 0 ? 0 : Math.ceilDiv(total, pageSize);
        return new SearchResponse(table.sourceTable(), table.auditTable(), pageNo, pageSize,
                rows.size(), total, totalPages, pageNo > 0,
                (long) pageNo + 1 < totalPages, rows);
    }

    private ChangeSummary summarize(List<AuditRevisionResponse> history) {
        long inserts = history.stream().filter(r -> r.operation() == RevisionOperation.INSERT).count();
        long updates = history.stream().filter(r -> r.operation() == RevisionOperation.UPDATE).count();
        long deletes = history.stream().filter(r -> r.operation() == RevisionOperation.DELETE).count();
        long unknown = history.size() - inserts - updates - deletes;
        Object firstRevision = history.isEmpty() ? null : history.getFirst().revision();
        Object latestRevision = history.isEmpty() ? null : history.getLast().revision();
        return new ChangeSummary(history.size(), inserts, updates, deletes, unknown,
                firstRevision, latestRevision);
    }

    private String key(Object id) {
        if (id == null) {
            throw new IllegalStateException(NULL_ENTITY_ID);
        }
        return id.toString();
    }
}
