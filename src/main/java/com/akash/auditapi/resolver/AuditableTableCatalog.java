package com.akash.auditapi.resolver;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.config.ApprovalProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.InvalidRequestException;
import com.akash.auditapi.exception.TableNotAllowedException;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.akash.auditapi.exception.ApiMessages.SOURCE_TABLE_REQUIRED;
import static com.akash.auditapi.exception.ApiMessages.duplicateTableLabel;

/**
 * Discovers source tables, creates public labels, and resolves accepted table inputs.
 * It replaces a hardcoded table registry with Oracle metadata lookup.
 */
@Component
public class AuditableTableCatalog {
    private final TableMetadataDao metadataDao;
    private final AuditApiProperties properties;
    private final ApprovalProperties approvalProperties;
    private final OracleIdentifierValidator identifierValidator;
    private final TableLabelFormatter labelFormatter;

    public AuditableTableCatalog(TableMetadataDao metadataDao, AuditApiProperties properties,
                                 ApprovalProperties approvalProperties,
                                 OracleIdentifierValidator identifierValidator,
                                 TableLabelFormatter labelFormatter) {
        this.metadataDao = metadataDao;
        this.properties = properties;
        this.approvalProperties = approvalProperties;
        this.identifierValidator = identifierValidator;
        this.labelFormatter = labelFormatter;
    }

    public List<String> labels() {
        return discover().values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public String resolve(String requestedTable) {
        String requested = requestedTable == null ? "" : requestedTable.strip();
        if (requested.toUpperCase(Locale.ROOT).endsWith(properties.auditSuffix())) {
            identifierValidator.normalizeTableName(requested);
            throw new InvalidRequestException(ApiOutcomeCode.AUDIT_TABLE_NOT_ACCEPTED,
                    SOURCE_TABLE_REQUIRED);
        }

        for (Map.Entry<String, String> table : discover().entrySet()) {
            if (matches(requested, table.getKey(), table.getValue())) {
                return table.getKey();
            }
        }

        String normalized = identifierValidator.normalizeTableName(requested);
        throw new TableNotAllowedException(normalized);
    }

    private Map<String, String> discover() {
        Map<String, String> tablesBySource = new LinkedHashMap<>();
        Map<String, String> sourceByLabel = new LinkedHashMap<>();
        for (String discoveredTable : metadataDao.findSourceTables(
                properties.sourceTablePrefix(), properties.auditSuffix())) {
            String source = identifierValidator.normalizeTableName(discoveredTable);
            if (approvalProperties.isApprovalTable(source)) {
                continue;
            }
            String label = labelFormatter.format(source, properties.sourceTablePrefix());
            String previous = sourceByLabel.putIfAbsent(label.toUpperCase(Locale.ROOT), source);
            if (previous != null && !previous.equals(source)) {
                throw new IllegalStateException(duplicateTableLabel(label));
            }
            tablesBySource.put(source, label);
        }
        return Collections.unmodifiableMap(tablesBySource);
    }

    private boolean matches(String requested, String source, String label) {
        String legacyTableName = source.substring(properties.sourceTablePrefix().length());
        String normalizedLabel = requested.replace(' ', '-');
        return requested.equalsIgnoreCase(source)
                || normalizedLabel.equalsIgnoreCase(label)
                || requested.equalsIgnoreCase(legacyTableName);
    }
}
