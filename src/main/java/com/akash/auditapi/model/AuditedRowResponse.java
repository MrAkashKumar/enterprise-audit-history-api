package com.akash.auditapi.model;
import java.util.Map;
import java.util.List;

public record AuditedRowResponse(
        Object id,
        boolean originalRecordPresent,
        Map<String, Object> originalData,
        ChangeSummary changeSummary,
        List<AuditRevisionResponse> auditHistory
) {}
