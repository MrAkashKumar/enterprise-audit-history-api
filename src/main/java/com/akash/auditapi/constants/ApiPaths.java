package com.akash.auditapi.constants;

/**
 * Centralizes REST path fragments used by the application controllers.
 * Keeping paths here prevents endpoint strings from being duplicated.
 */
public final class ApiPaths {
    public static final String V1 = "/api/v1";
    public static final String TABLE_AUDIT = "/{tableName}";
    public static final String ALL_TABLES = "/allTable";
    public static final String HOLIDAYS = "/holidays";
    public static final String RESOURCE_ID = "/{id}";

    private ApiPaths() {
    }
}
