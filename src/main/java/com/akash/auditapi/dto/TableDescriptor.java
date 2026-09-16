package com.akash.auditapi.dto;

/**
 * Describes a verified source/audit table pair and its required columns.
 * DAO SQL builders consume this trusted metadata instead of caller-provided identifiers.
 */
public record TableDescriptor(
        String sourceTable,
        String auditTable,
        String idColumn,
        String auditOrderColumn,
        String revisionTypeColumn) {}
