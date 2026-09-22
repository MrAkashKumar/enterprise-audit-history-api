package com.akash.auditapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.akash.auditapi.constants.AuditDefaults.CHECKER_USERNAME_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.ID_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.MAKER_USERNAME_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.ORACLE_IDENTIFIER_REGEX;
import static com.akash.auditapi.exception.ApiMessages.invalidConfiguration;

/**
 * Defines the required columns for optional approval-table enrichment.
 * Approval table suffixes are fixed application conventions.
 */
@ConfigurationProperties(prefix = "audit-api.approval")
public record ApprovalProperties(String idColumn, String makerUsernameColumn,
                                 String checkerUsernameColumn) {
    public ApprovalProperties {
        idColumn = normalizeOrDefault(idColumn, ID_COLUMN, "id-column");
        makerUsernameColumn = normalizeOrDefault(
                makerUsernameColumn, MAKER_USERNAME_COLUMN, "maker-username-column");
        checkerUsernameColumn = normalizeOrDefault(
                checkerUsernameColumn, CHECKER_USERNAME_COLUMN, "checker-username-column");

    }

    private static String normalizeOrDefault(String value, String fallback, String property) {
        return normalize(value == null || value.isBlank() ? fallback : value,
                ORACLE_IDENTIFIER_REGEX, property);
    }

    private static String normalize(String value, String regex, String property) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!Pattern.matches(regex, normalized)) {
            throw new IllegalArgumentException(invalidConfiguration("approval." + property, value));
        }
        return normalized;
    }
}
