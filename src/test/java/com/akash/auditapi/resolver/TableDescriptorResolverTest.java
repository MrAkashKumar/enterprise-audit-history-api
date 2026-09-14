package com.akash.auditapi.resolver;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.exception.ApiErrorCode;
import com.akash.auditapi.exception.AuditApiException;
import com.akash.auditapi.model.TableDescriptor;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class TableDescriptorResolverTest {
    private final TableMetadataDao metadataDao = mock(TableMetadataDao.class);
    private final AuditApiProperties properties = new AuditApiProperties(
            "_AUD", "ID", "REV", "REVTYPE", Set.of("PMC_POSITION_BALANCE"), 200);
    private final TableDescriptorResolver resolver = new TableDescriptorResolver(
            metadataDao, properties, new OracleIdentifierValidator());

    @Test
    void resolvesAllowlistedCompleteEnversTablePair() {
        when(metadataDao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(completeColumns());

        assertThat(resolver.resolve("pmc_position_balance")).isEqualTo(new TableDescriptor(
                "PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE"));
    }

    @Test
    void cachesVerifiedMetadataForRepeatedRequests() {
        when(metadataDao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(completeColumns());

        resolver.resolve("PMC_POSITION_BALANCE");
        resolver.resolve("pmc_position_balance");

        verify(metadataDao, times(1))
                .findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD");
        verify(metadataDao, times(1))
                .findColumnsByTable("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD");
    }

    @Test
    void rejectsAuditNameDisallowedTableMissingPairAndMissingColumn() {
        assertCode("PMC_POSITION_BALANCE_AUD", ApiErrorCode.AUDIT_TABLE_NOT_ACCEPTED);
        assertCode("PMC_SECRET", ApiErrorCode.TABLE_NOT_ALLOWED);

        when(metadataDao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("PMC_POSITION_BALANCE"));
        assertCode("PMC_POSITION_BALANCE", ApiErrorCode.TABLE_PAIR_NOT_FOUND);

        when(metadataDao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Map.of(
                        "PMC_POSITION_BALANCE", Set.of("VERSION"),
                        "PMC_POSITION_BALANCE_AUD", Set.of("ID", "REV", "REVTYPE")));
        assertCode("PMC_POSITION_BALANCE", ApiErrorCode.MISSING_REQUIRED_COLUMN);
    }

    @Test
    void supportsEmptyAllowlistAndDetectsMissingSourceTable() {
        AuditApiProperties unrestrictedProperties = new AuditApiProperties(
                "_AUD", "ID", "REV", "REVTYPE", Set.of(), 200);
        TableDescriptorResolver unrestricted = new TableDescriptorResolver(
                metadataDao, unrestrictedProperties, new OracleIdentifierValidator());
        when(metadataDao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(completeColumns());

        assertThat(unrestricted.resolve("PMC_POSITION_BALANCE")).isNotNull();

        TableDescriptorResolver missingSourceResolver = new TableDescriptorResolver(
                metadataDao, properties, new OracleIdentifierValidator());
        when(metadataDao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("PMC_POSITION_BALANCE_AUD"));
        assertThatThrownBy(() -> missingSourceResolver.resolve("PMC_POSITION_BALANCE"))
                .isInstanceOfSatisfying(AuditApiException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ApiErrorCode.TABLE_PAIR_NOT_FOUND));
    }

    private Map<String, Set<String>> completeColumns() {
        return Map.of(
                "PMC_POSITION_BALANCE", Set.of("ID"),
                "PMC_POSITION_BALANCE_AUD", Set.of("ID", "REV", "REVTYPE"));
    }

    private void assertCode(String table, ApiErrorCode code) {
        assertThatThrownBy(() -> resolver.resolve(table))
                .isInstanceOfSatisfying(AuditApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
