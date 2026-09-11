package com.akash.auditapi.model;
public record TableDescriptor(
        String sourceTable,
        String auditTable,
        String idColumn,
        String auditOrderColumn,
        String revisionTypeColumn) {}
