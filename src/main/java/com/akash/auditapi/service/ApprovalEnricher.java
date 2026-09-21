package com.akash.auditapi.service;

import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.response.ApprovalResponse;
import com.akash.auditapi.dto.response.AuditedRowResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.akash.auditapi.exception.ApiMessages.NULL_ENTITY_ID;
import static com.akash.auditapi.exception.ApiMessages.duplicateApprovalRecord;

/**
 * Adds optional maker/checker data to already assembled audit rows.
 * Approval ID normalization is deliberately isolated from source/audit grouping.
 */
@Component
public class ApprovalEnricher {
    public List<AuditedRowResponse> enrich(List<AuditedRowResponse> rows,
                                            List<ApprovalRecord> approvalRecords) {
        if (approvalRecords.isEmpty()) {
            return rows;
        }
        Map<String, ApprovalResponse> approvalById = index(approvalRecords);
        return rows.stream().map(row -> copyWithApproval(row,
                approvalById.getOrDefault(key(row.id()), ApprovalResponse.ABSENT))).toList();
    }

    private Map<String, ApprovalResponse> index(List<ApprovalRecord> records) {
        Map<String, ApprovalResponse> approvalById = new HashMap<>();
        for (ApprovalRecord record : records) {
            ApprovalResponse previous = approvalById.putIfAbsent(key(record.id()),
                    ApprovalResponse.present(record.makerUsername(), record.checkerUsername()));
            if (previous != null) {
                throw new IllegalStateException(duplicateApprovalRecord(record.id()));
            }
        }
        return approvalById;
    }

    private AuditedRowResponse copyWithApproval(AuditedRowResponse row, ApprovalResponse approval) {
        return new AuditedRowResponse(row.id(), row.originalRecordPresent(), row.originalData(),
                approval, row.changeSummary(), row.auditHistory());
    }

    private String key(Object id) {
        if (id == null) {
            throw new IllegalStateException(NULL_ENTITY_ID);
        }
        if (id instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        }
        return id.toString();
    }
}
