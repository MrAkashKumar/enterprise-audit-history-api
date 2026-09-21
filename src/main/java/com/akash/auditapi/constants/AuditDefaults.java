package com.akash.auditapi.constants;

import java.util.List;

/**
 * Defines shared audit conventions, pagination defaults, and Oracle limits.
 * Configuration classes use these values when no external override is supplied.
 */
public final class AuditDefaults {
    public static final String AUDIT_SUFFIX = "_AUD";
    public static final String SOURCE_TABLE_PREFIX = "PMC_";
    public static final String ID_COLUMN = "ID";
    public static final String REVISION_COLUMN = "REV";
    public static final String REVISION_TYPE_COLUMN = "REVTYPE";
    public static final List<String> APPROVAL_SUFFIXES =
            List.of("_APPROVAL_REQUEST", "_APPROVAL");
    public static final String MAKER_USERNAME_COLUMN = "MAKER_USERNAME";
    public static final String CHECKER_USERNAME_COLUMN = "CHECKER_USERNAME";
    public static final int DEFAULT_MAX_PAGE_SIZE = 200;
    public static final String DEFAULT_PAGE_NUMBER_TEXT = "0";
    public static final String DEFAULT_PAGE_SIZE_TEXT = "10";
    public static final int ORACLE_IN_LIMIT = 1000;
    public static final String ORACLE_IDENTIFIER_REGEX = "[A-Z][A-Z0-9_$#]{0,127}";
    public static final String AUDIT_SUFFIX_REGEX = "_[A-Z0-9_$#]{1,127}";

    private AuditDefaults() {
    }
}
