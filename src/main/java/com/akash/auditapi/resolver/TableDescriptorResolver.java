package com.akash.auditapi.resolver;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.exception.AuditTableNotFoundException;
import com.akash.auditapi.model.ApiOutcomeCode;
import com.akash.auditapi.exception.InvalidRequestException;
import com.akash.auditapi.exception.MissingAuditColumnException;
import com.akash.auditapi.exception.TableNotAllowedException;
import com.akash.auditapi.model.TableDescriptor;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.akash.auditapi.exception.ApiMessages.SOURCE_TABLE_REQUIRED;
import static com.akash.auditapi.exception.ApiMessages.tablePairNotFound;

@Component
public class TableDescriptorResolver {
    private final TableMetadataDao metadataDao;
    private final AuditApiProperties properties;
    private final OracleIdentifierValidator identifierValidator;
    private final ConcurrentMap<String, TableDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public TableDescriptorResolver(TableMetadataDao metadataDao, AuditApiProperties properties,
                                   OracleIdentifierValidator identifierValidator) {
        this.metadataDao = metadataDao;
        this.properties = properties;
        this.identifierValidator = identifierValidator;
    }

    public TableDescriptor resolve(String requestedTable) {
        String source = identifierValidator.normalizeTableName(requestedTable);
        if (source.endsWith(properties.auditSuffix())) {
            throw new InvalidRequestException(ApiOutcomeCode.AUDIT_TABLE_NOT_ACCEPTED,
                    SOURCE_TABLE_REQUIRED);
        }
        if (!properties.allowedTables().isEmpty() && !properties.allowedTables().contains(source)) {
            throw new TableNotAllowedException(source);
        }

        return descriptorCache.computeIfAbsent(source, this::resolveVerifiedDescriptor);
    }

    private TableDescriptor resolveVerifiedDescriptor(String source) {
        String audit = identifierValidator.normalizeTableName(source + properties.auditSuffix());
        Set<String> existingTables = metadataDao.findExistingTables(source, audit);
        if (!existingTables.contains(source) || !existingTables.contains(audit)) {
            throw new AuditTableNotFoundException(tablePairNotFound(source, audit));
        }

        Map<String, Set<String>> columnsByTable = metadataDao.findColumnsByTable(source, audit);
        requireColumn(columnsByTable, source, properties.idColumn());
        requireColumn(columnsByTable, audit, properties.idColumn());
        requireColumn(columnsByTable, audit, properties.auditOrderColumn());
        requireColumn(columnsByTable, audit, properties.revisionTypeColumn());
        return new TableDescriptor(source, audit, properties.idColumn(),
                properties.auditOrderColumn(), properties.revisionTypeColumn());
    }

    private void requireColumn(Map<String, Set<String>> columnsByTable, String table, String column) {
        if (!columnsByTable.getOrDefault(table, Set.of()).contains(column)) {
            throw new MissingAuditColumnException(table, column);
        }
    }
}
