package com.akash.auditapi.exception;

/**
 * Centralizes safe client messages, validation text, and structured log templates.
 * It prevents response and logging strings from being duplicated across layers.
 */
public final class ApiMessages {
    public static final String REQUEST_SUCCESSFUL = "Request completed successfully";
    public static final String INVALID_VALUE = "Invalid value";
    public static final String VALIDATION_FAILED = "Request validation failed";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String DATABASE_QUERY_FAILED = "The database query could not be completed";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred";
    public static final String AUDIT_TABLE_LIST_LOG =
            "Audit table discovery completed [tableCount={}]";
    public static final String AUDIT_SEARCH_STARTED_LOG =
            "Audit history retrieval started [table={}, pageNo={}, pageSize={}]";
    public static final String AUDIT_SEARCH_COMPLETED_LOG =
            "Audit history retrieval completed [table={}, returnedRows={}, totalElements={}]";
    public static final String TABLE_METADATA_VERIFIED_LOG =
            "Audit table metadata verified [sourceTable={}, auditTable={}]";
    public static final String APPROVAL_METADATA_VERIFIED_LOG =
            "Approval table metadata verified [sourceTable={}, approvalTable={}]";
    public static final String AMBIGUOUS_APPROVAL_TABLE =
            "Multiple approval tables found for source table: ";
    public static final String DATABASE_FAILURE_LOG =
            "Database operation failed [exceptionType={}]";
    public static final String UNEXPECTED_FAILURE_LOG =
            "Unexpected failure [applicationCode={}, exceptionType={}]";
    public static final String NULL_ENTITY_ID = "Database row contains a null entity ID";
    public static final String LOB_TOO_LARGE = "LOB value is too large for a JSON response";
    public static final String SIMPLE_ORACLE_IDENTIFIER_REQUIRED =
            "tableName must be a simple Oracle identifier";
    public static final String SOURCE_TABLE_REQUIRED =
            "Pass the source table name, not the audit table name";
    public static final String INVALID_PAGE_NUMBER = "pageNo must be zero or greater";

    private ApiMessages() {
    }

    public static String invalidPageSize(int maximum) {
        return "pageSize must be between 1 and " + maximum;
    }

    public static String tableNotExposed(String table) {
        return "Table is not exposed by this API: " + table;
    }

    public static String tablePairNotFound(String source, String audit) {
        return "Source/audit table pair not found: " + source + ", " + audit;
    }

    public static String requiredColumnNotFound(String table, String column) {
        return "Required column " + column + " not found in " + table;
    }

    public static String holidayNotFound(Long id) {
        return "Holiday not found: " + id;
    }

    public static String maximumPageSizeExceeded(int maximum) {
        return "audit-api.max-page-size cannot exceed " + maximum;
    }

    public static String invalidConfiguration(String property, String value) {
        return "Invalid audit-api." + property + ": " + value;
    }

    public static String duplicateTableLabel(String publicName) {
        return "Duplicate table label: " + publicName;
    }

    public static String configuredApprovalTableNotFound(String source, String approval) {
        return "Configured approval table not found for " + source + ": " + approval;
    }

    public static String duplicateApprovalRecord(Object id) {
        return "Multiple approval rows found for source ID: " + id;
    }
}
