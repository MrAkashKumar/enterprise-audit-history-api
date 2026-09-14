package com.akash.auditapi.dto;

public record ChangeSummary(
        long totalRevisions,
        long insertCount,
        long updateCount,
        long deleteCount,
        long unknownCount,
        Object firstRevision,
        Object latestRevision
) {
    public static final ChangeSummary EMPTY = new ChangeSummary(0, 0, 0, 0, 0, null, null);
}
