package com.akash.auditapi.dto;
public record TableDescriptor(
        String sourceTable,
        String auditTable,
        String idColumn,
        String auditOrderColumn,
        String revisionTypeColumn) {}
