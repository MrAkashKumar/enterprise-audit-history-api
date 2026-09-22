package com.akash.auditapi.resolver;

import com.akash.auditapi.config.ApprovalProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import com.akash.auditapi.exception.MissingAuditColumnException;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.akash.auditapi.exception.ApiMessages.AMBIGUOUS_APPROVAL_TABLE;
import static com.akash.auditapi.exception.ApiMessages.APPROVAL_METADATA_VERIFIED_LOG;
import static com.akash.auditapi.constants.AuditDefaults.APPROVAL_SUFFIXES;

/**
 * Resolves an optional approval table and verifies its minimal projection columns.
 * Both positive and absent metadata results are cached per source table.
 */
@Component
@Slf4j
public class ApprovalTableResolver {
    private final TableMetadataDao metadataDao;
    private final ApprovalProperties properties;
    private final OracleIdentifierValidator identifierValidator;
    private final ConcurrentMap<String, Optional<ApprovalTableDescriptor>> cache =
            new ConcurrentHashMap<>();

    public ApprovalTableResolver(TableMetadataDao metadataDao, ApprovalProperties properties,
                                 OracleIdentifierValidator identifierValidator) {
        this.metadataDao = metadataDao;
        this.properties = properties;
        this.identifierValidator = identifierValidator;
    }

    public Optional<ApprovalTableDescriptor> resolve(String sourceTable) {
        String source = identifierValidator.normalizeTableName(sourceTable);
        return cache.computeIfAbsent(source, this::resolveUncached);
    }

    private Optional<ApprovalTableDescriptor> resolveUncached(String source) {
        if (APPROVAL_SUFFIXES.stream().anyMatch(source::endsWith)) {
            return Optional.empty();
        }
        String table = null;
        Set<String> columns = Set.of();
        for (String suffix : APPROVAL_SUFFIXES) {
            String candidate = identifierValidator.normalizeTableName(source + suffix);
            Set<String> candidateColumns = metadataDao.findColumns(candidate);
            if (!candidateColumns.isEmpty()) {
                if (table != null) {
                    throw new IllegalStateException(AMBIGUOUS_APPROVAL_TABLE + source);
                }
                table = candidate;
                columns = candidateColumns;
            }
        }
        if (table == null) {
            return Optional.empty();
        }

        requireColumn(columns, table, properties.idColumn());
        requireColumn(columns, table, properties.makerUsernameColumn());
        requireColumn(columns, table, properties.checkerUsernameColumn());
        log.info(APPROVAL_METADATA_VERIFIED_LOG, source, table);
        return Optional.of(new ApprovalTableDescriptor(table, properties.idColumn(),
                properties.makerUsernameColumn(), properties.checkerUsernameColumn()));
    }

    private void requireColumn(Set<String> columns, String table, String column) {
        if (!columns.contains(column)) {
            throw new MissingAuditColumnException(table, column);
        }
    }
}
