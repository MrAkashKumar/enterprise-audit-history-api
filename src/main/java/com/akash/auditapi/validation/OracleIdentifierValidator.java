package com.akash.auditapi.validation;

import com.akash.auditapi.exception.InvalidRequestException;
import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.akash.auditapi.constants.AuditDefaults.ORACLE_IDENTIFIER_REGEX;
import static com.akash.auditapi.exception.ApiMessages.SIMPLE_ORACLE_IDENTIFIER_REQUIRED;

/**
 * Normalizes and validates unquoted Oracle table identifiers before dynamic SQL use.
 * Unsafe or malformed names are rejected with the standard client error.
 */
@Component
public class OracleIdentifierValidator {
    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile(ORACLE_IDENTIFIER_REGEX);

    public String normalizeTableName(String requestedTable) {
        String normalized = requestedTable == null ? "" : requestedTable.strip().toUpperCase(Locale.ROOT);
        if (!SIMPLE_IDENTIFIER.matcher(normalized).matches()) {
            throw new InvalidRequestException(ApiOutcomeCode.INVALID_TABLE_NAME,
                    SIMPLE_ORACLE_IDENTIFIER_REQUIRED);
        }
        return normalized;
    }
}
