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
 * Only verified descriptors are cached, so a table created after startup is discovered.
 */
@Component
@Slf4j
public class ApprovalTableResolver {
    private final TableMetadataDao metadataDao;
    private final ApprovalProperties properties;
    private final OracleIdentifierValidator identifierValidator;
    private final ConcurrentMap<String, ApprovalTableDescriptor> cache = new ConcurrentHashMap<>();

    public ApprovalTableResolver(TableMetadataDao metadataDao, ApprovalProperties properties,
                                 OracleIdentifierValidator identifierValidator) {
        this.metadataDao = metadataDao;
        this.properties = properties;
        this.identifierValidator = identifierValidator;
    }

    public Optional<ApprovalTableDescriptor> resolve(String sourceTable) {
        String source = identifierValidator.normalizeTableName(sourceTable);
        ApprovalTableDescriptor cached = cache.get(source);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<ApprovalTableDescriptor> resolved = resolveUncached(source);
        if (resolved.isEmpty()) {
            return resolved;
        }
        ApprovalTableDescriptor descriptor = resolved.get();
        cache.putIfAbsent(source, descriptor);
        return Optional.of(cache.get(source));
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
        requireColumn(columns, table, properties.checkerUsernameColumn());
        boolean makerUsernameColumnPresent = columns.contains(properties.makerUsernameColumn());
        log.info(APPROVAL_METADATA_VERIFIED_LOG, source, table);
        return Optional.of(new ApprovalTableDescriptor(table, properties.idColumn(),
                properties.makerUsernameColumn(), properties.checkerUsernameColumn(),
                makerUsernameColumnPresent));
    }

    private void requireColumn(Set<String> columns, String table, String column) {
        if (!columns.contains(column)) {
            throw new MissingAuditColumnException(table, column);
        }
    }
}
