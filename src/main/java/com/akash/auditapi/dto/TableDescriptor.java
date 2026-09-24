package com.akash.auditapi.dto;

/**
 * Describes a verified source table and its optional audit-table metadata.
 * DAO SQL builders consume this trusted metadata instead of caller-provided identifiers.
 */
public record TableDescriptor(
        String sourceTable,
        String auditTable,
        String idColumn,
        String auditOrderColumn,
        String revisionTypeColumn) {

    public boolean hasAuditHistory() {
        return auditTable != null;
    }
}
