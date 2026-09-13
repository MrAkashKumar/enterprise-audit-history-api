package com.akash.auditapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import static com.akash.auditapi.config.AuditDefaults.AUDIT_SUFFIX;
import static com.akash.auditapi.config.AuditDefaults.AUDIT_SUFFIX_REGEX;
import static com.akash.auditapi.config.AuditDefaults.ID_COLUMN;
import static com.akash.auditapi.config.AuditDefaults.DEFAULT_MAX_PAGE_SIZE;
import static com.akash.auditapi.config.AuditDefaults.ORACLE_IDENTIFIER_REGEX;
import static com.akash.auditapi.config.AuditDefaults.ORACLE_IN_LIMIT;
import static com.akash.auditapi.config.AuditDefaults.REVISION_COLUMN;
import static com.akash.auditapi.config.AuditDefaults.REVISION_TYPE_COLUMN;
import static com.akash.auditapi.exception.ApiMessages.invalidConfiguration;
import static com.akash.auditapi.exception.ApiMessages.maximumPageSizeExceeded;

@ConfigurationProperties(prefix = "audit-api")
public record AuditApiProperties(String auditSuffix, String idColumn, String auditOrderColumn,
                                 String revisionTypeColumn,
                                 Set<String> allowedTables, int maxPageSize) {
    public AuditApiProperties {
        auditSuffix = valueOrDefault(auditSuffix, AUDIT_SUFFIX).toUpperCase(Locale.ROOT);
        idColumn = valueOrDefault(idColumn, ID_COLUMN).toUpperCase(Locale.ROOT);
        auditOrderColumn = valueOrDefault(auditOrderColumn, REVISION_COLUMN).toUpperCase(Locale.ROOT);
        revisionTypeColumn = valueOrDefault(revisionTypeColumn, REVISION_TYPE_COLUMN).toUpperCase(Locale.ROOT);
        allowedTables = allowedTables == null ? Set.of() : allowedTables.stream()
                .map(table -> table.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        maxPageSize = maxPageSize <= 0 ? DEFAULT_MAX_PAGE_SIZE : maxPageSize;
        if (maxPageSize > ORACLE_IN_LIMIT) {
            throw new IllegalArgumentException(maximumPageSizeExceeded(ORACLE_IN_LIMIT));
        }
        validate(auditSuffix, AUDIT_SUFFIX_REGEX, "audit-suffix");
        validate(idColumn, ORACLE_IDENTIFIER_REGEX, "id-column");
        validate(auditOrderColumn, ORACLE_IDENTIFIER_REGEX, "audit-order-column");
        validate(revisionTypeColumn, ORACLE_IDENTIFIER_REGEX, "revision-type-column");
        allowedTables.forEach(table -> validate(table, ORACLE_IDENTIFIER_REGEX, "allowed-tables"));
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
