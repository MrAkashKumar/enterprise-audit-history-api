package com.akash.auditapi.dto.response;

import com.akash.auditapi.dto.ChangeSummary;
import com.akash.auditapi.dto.DatabaseRowSnapshot;

import java.util.List;
import java.util.Map;

/**
 * Groups a current source row, revision summary, and complete history by entity ID.
 * It also represents deleted entities whose current source row is absent.
 */
public record AuditedRowResponse(
        Object id,
        boolean originalRecordPresent,
        Map<String, Object> originalData,
        ApprovalResponse approval,
        ChangeSummary changeSummary,
        List<AuditRevisionResponse> auditHistory
) {
    public AuditedRowResponse {
        originalData = originalData == null ? null : DatabaseRowSnapshot.copyOf(originalData);
        approval = approval == null ? ApprovalResponse.ABSENT : approval;
        auditHistory = List.copyOf(auditHistory);
    }
}
