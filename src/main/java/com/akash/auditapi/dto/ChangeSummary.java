package com.akash.auditapi.dto;

/**
 * Summarizes revision counts and the first/latest revision for one entity.
 * It is embedded in each audited-row response.
 */
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
