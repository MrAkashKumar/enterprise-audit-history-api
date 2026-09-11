package com.akash.auditapi.model;

public record ChangeSummary(
        long totalRevisions,
        long insertCount,
        long updateCount,
        long deleteCount,
        long unknownCount,
        Object firstRevision,
        Object latestRevision
) {}
