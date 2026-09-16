package com.akash.auditapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.akash.auditapi.constants.AuditDefaults.AUDIT_SUFFIX;
import static com.akash.auditapi.constants.AuditDefaults.AUDIT_SUFFIX_REGEX;
import static com.akash.auditapi.constants.AuditDefaults.DEFAULT_MAX_PAGE_SIZE;
import static com.akash.auditapi.constants.AuditDefaults.ID_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.ORACLE_IDENTIFIER_REGEX;
import static com.akash.auditapi.constants.AuditDefaults.ORACLE_IN_LIMIT;
import static com.akash.auditapi.constants.AuditDefaults.REVISION_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.REVISION_TYPE_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.SOURCE_TABLE_PREFIX;
import static com.akash.auditapi.exception.ApiMessages.invalidConfiguration;
import static com.akash.auditapi.exception.ApiMessages.maximumPageSizeExceeded;

/**
 * Holds validated settings for dynamic source and audit-table access.
 * Spring binds these values from the {@code audit-api} configuration namespace.
 */
@ConfigurationProperties(prefix = "audit-api")
public record AuditApiProperties(String sourceTablePrefix, String auditSuffix, String idColumn,
                                 String auditOrderColumn, String revisionTypeColumn,
                                 int maxPageSize) {
    public AuditApiProperties {
        sourceTablePrefix = valueOrDefault(sourceTablePrefix, SOURCE_TABLE_PREFIX)
                .toUpperCase(Locale.ROOT);
        auditSuffix = valueOrDefault(auditSuffix, AUDIT_SUFFIX).toUpperCase(Locale.ROOT);
        idColumn = valueOrDefault(idColumn, ID_COLUMN).toUpperCase(Locale.ROOT);
        auditOrderColumn = valueOrDefault(auditOrderColumn, REVISION_COLUMN).toUpperCase(Locale.ROOT);
        revisionTypeColumn = valueOrDefault(revisionTypeColumn, REVISION_TYPE_COLUMN).toUpperCase(Locale.ROOT);
        maxPageSize = maxPageSize <= 0 ? DEFAULT_MAX_PAGE_SIZE : maxPageSize;
        if (maxPageSize > ORACLE_IN_LIMIT) {
            throw new IllegalArgumentException(maximumPageSizeExceeded(ORACLE_IN_LIMIT));
        }
        validate(sourceTablePrefix, ORACLE_IDENTIFIER_REGEX, "source-table-prefix");
        validate(auditSuffix, AUDIT_SUFFIX_REGEX, "audit-suffix");
        validate(idColumn, ORACLE_IDENTIFIER_REGEX, "id-column");
        validate(auditOrderColumn, ORACLE_IDENTIFIER_REGEX, "audit-order-column");
        validate(revisionTypeColumn, ORACLE_IDENTIFIER_REGEX, "revision-type-column");
    }
    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void validate(String value, String regex, String property) {
        if (!Pattern.matches(regex, value)) {
            throw new IllegalArgumentException(invalidConfiguration(property, value));
        }
    }
}
