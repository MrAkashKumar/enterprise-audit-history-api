package com.akash.auditapi.service;

import com.akash.auditapi.dto.ChangeSummary;
import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.TableDescriptor;
import com.akash.auditapi.dto.response.AuditRevisionResponse;
import com.akash.auditapi.dto.response.AuditedRowResponse;
import com.akash.auditapi.dto.response.ApprovalResponse;
import com.akash.auditapi.enums.RevisionOperation;
import com.akash.auditapi.resolver.RevisionOperationResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.akash.auditapi.exception.ApiMessages.NULL_ENTITY_ID;
import static com.akash.auditapi.exception.ApiMessages.duplicateApprovalRecord;

/**
 * Converts database rows into stable responses grouped by entity ID.
 * It orders revisions, resolves operations, and calculates change summaries in one pass.
 */
@Component
public class AuditHistoryAssembler {
    private final RevisionOperationResolver operationResolver;

    public AuditHistoryAssembler(RevisionOperationResolver operationResolver) {
        this.operationResolver = operationResolver;
    }

    public List<AuditedRowResponse> assemble(TableDescriptor table, List<Object> ids,
                                              List<Map<String, Object>> sourceRows,
                                              List<Map<String, Object>> auditRows,
                                              List<ApprovalRecord> approvalRecords) {
        Map<String, Map<String, Object>> sourceById = indexSourceRows(table, sourceRows);
        Map<String, HistoryAccumulator> historyById = indexHistory(table, auditRows);
        Map<String, ApprovalResponse> approvalById = indexApprovals(approvalRecords);

        List<AuditedRowResponse> rows = new ArrayList<>(ids.size());
        for (Object id : ids) {
            String entityKey = entityKey(id);
            Map<String, Object> originalData = sourceById.get(entityKey);
            HistoryAccumulator history = historyById.get(entityKey);
            rows.add(new AuditedRowResponse(id, originalData != null, originalData,
                    approvalById.getOrDefault(entityKey, ApprovalResponse.ABSENT),
                    history == null ? ChangeSummary.EMPTY : history.summary(),
                    history == null ? List.of() : history.revisions()));
        }
        return rows;
    }

    private Map<String, ApprovalResponse> indexApprovals(List<ApprovalRecord> approvalRecords) {
        Map<String, ApprovalResponse> approvalById = new HashMap<>();
        for (ApprovalRecord record : approvalRecords) {
            String key = entityKey(record.id());
            ApprovalResponse previous = approvalById.putIfAbsent(key,
                    ApprovalResponse.present(record.makerUsername(), record.checkerUsername()));
            if (previous != null) {
                throw new IllegalStateException(duplicateApprovalRecord(record.id()));
            }
        }
        return approvalById;
    }

    private Map<String, Map<String, Object>> indexSourceRows(
            TableDescriptor table, List<Map<String, Object>> sourceRows) {
        Map<String, Map<String, Object>> sourceById = new HashMap<>();
        for (Map<String, Object> row : sourceRows) {
            sourceById.put(entityKey(row.get(table.idColumn())), row);
        }
        return sourceById;
    }

    private Map<String, HistoryAccumulator> indexHistory(
            TableDescriptor table, List<Map<String, Object>> auditRows) {
        Map<String, HistoryAccumulator> historyById = new HashMap<>();
        for (Map<String, Object> row : auditRows) {
            String entityKey = entityKey(row.get(table.idColumn()));
            HistoryAccumulator history = historyById.computeIfAbsent(
                    entityKey, ignored -> new HistoryAccumulator());
            RevisionOperation operation = operationResolver.resolve(row.get(table.revisionTypeColumn()));
            history.add(new AuditRevisionResponse(
                    history.size() + 1, row.get(table.auditOrderColumn()),
                    operation.code(), operation, row));
        }
        return historyById;
    }

    private String entityKey(Object id) {
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
            if (revision.operation() == RevisionOperation.INSERT) {
                inserts++;
            } else if (revision.operation() == RevisionOperation.UPDATE) {
                updates++;
            } else if (revision.operation() == RevisionOperation.DELETE) {
                deletes++;
            } else {
                unknown++;
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
