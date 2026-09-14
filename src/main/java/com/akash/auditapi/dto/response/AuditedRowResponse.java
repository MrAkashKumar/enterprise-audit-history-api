package com.akash.auditapi.dto.response;

import com.akash.auditapi.dto.ChangeSummary;
import com.akash.auditapi.dto.DatabaseRowSnapshot;

import java.util.List;
import java.util.Map;

public record AuditedRowResponse(
        Object id,
        boolean originalRecordPresent,
        Map<String, Object> originalData,
        ChangeSummary changeSummary,
        List<AuditRevisionResponse> auditHistory
) {
    public AuditedRowResponse {
        originalData = originalData == null ? null : DatabaseRowSnapshot.copyOf(originalData);
        auditHistory = List.copyOf(auditHistory);
    }
}
