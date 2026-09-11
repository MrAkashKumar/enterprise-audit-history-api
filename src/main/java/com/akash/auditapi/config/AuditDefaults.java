package com.akash.auditapi.config;

public final class AuditDefaults {
    public static final String AUDIT_SUFFIX = "_AUD";
    public static final String ID_COLUMN = "ID";
    public static final String REVISION_COLUMN = "REV";
    public static final String REVISION_TYPE_COLUMN = "REVTYPE";
    public static final int DEFAULT_MAX_PAGE_SIZE = 200;
    public static final String DEFAULT_PAGE_NUMBER_TEXT = "0";
    public static final String DEFAULT_PAGE_SIZE_TEXT = "10";
    public static final String DEFAULT_API_KEY_HEADER = "X-API-Key";
    public static final int ORACLE_IN_LIMIT = 1000;
    public static final String ORACLE_IDENTIFIER_REGEX = "[A-Z][A-Z0-9_$#]{0,127}";
    public static final String AUDIT_SUFFIX_REGEX = "_[A-Z0-9_$#]{1,127}";

    private AuditDefaults() {
    }
}
