package com.akash.auditapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static com.akash.auditapi.constants.AuditDefaults.APPROVAL_SUFFIXES;
import static com.akash.auditapi.constants.AuditDefaults.AUDIT_SUFFIX_REGEX;
import static com.akash.auditapi.constants.AuditDefaults.CHECKER_USERNAME_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.ID_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.MAKER_USERNAME_COLUMN;
import static com.akash.auditapi.constants.AuditDefaults.ORACLE_IDENTIFIER_REGEX;
import static com.akash.auditapi.exception.ApiMessages.invalidConfiguration;

/**
 * Defines the optional approval-table naming conventions and required columns.
 * Table overrides handle safe exceptions where a source and approval base name differ.
 */
@ConfigurationProperties(prefix = "audit-api.approval")
public record ApprovalProperties(List<String> suffixes, String idColumn,
                                 String makerUsernameColumn, String checkerUsernameColumn,
                                 Map<String, String> tableOverrides) {
    public ApprovalProperties {
        suffixes = suffixes == null || suffixes.isEmpty() ? APPROVAL_SUFFIXES : suffixes;
        suffixes = suffixes.stream().map(value -> normalize(value, AUDIT_SUFFIX_REGEX, "suffixes"))
                .distinct().toList();
        idColumn = normalizeOrDefault(idColumn, ID_COLUMN, "id-column");
        makerUsernameColumn = normalizeOrDefault(
                makerUsernameColumn, MAKER_USERNAME_COLUMN, "maker-username-column");
        checkerUsernameColumn = normalizeOrDefault(
                checkerUsernameColumn, CHECKER_USERNAME_COLUMN, "checker-username-column");

        Map<String, String> normalizedOverrides = new LinkedHashMap<>();
        if (tableOverrides != null) {
            tableOverrides.forEach((source, approval) -> normalizedOverrides.put(
                    normalize(source, ORACLE_IDENTIFIER_REGEX, "table-overrides"),
                    normalize(approval, ORACLE_IDENTIFIER_REGEX, "table-overrides")));
        }
        tableOverrides = Map.copyOf(normalizedOverrides);
    }

    public boolean isApprovalTable(String tableName) {
        String normalized = tableName.toUpperCase(Locale.ROOT);
        return suffixes.stream().anyMatch(normalized::endsWith)
                || tableOverrides.containsValue(normalized);
    }

    private static String normalizeOrDefault(String value, String fallback, String property) {
        return normalize(value == null || value.isBlank() ? fallback : value,
                ORACLE_IDENTIFIER_REGEX, property);
    }

    private static String normalize(String value, String regex, String property) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!Pattern.matches(regex, normalized)) {
            throw new IllegalArgumentException(invalidConfiguration("approval." + property, value));
        }
        return normalized;
    }
}
