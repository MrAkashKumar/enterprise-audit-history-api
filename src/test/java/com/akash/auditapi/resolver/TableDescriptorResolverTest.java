package com.akash.auditapi.resolver;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.AuditApiException;
import com.akash.auditapi.dto.TableDescriptor;
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
            "PMC_", "_AUD", "ID", "REV", "REVTYPE", 200);
    private final TableDescriptorResolver resolver = new TableDescriptorResolver(
            metadataDao, properties, new OracleIdentifierValidator());

    @Test
    void resolvesCompleteEnversTablePair() {
        when(metadataDao.findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("POSITION_BALANCE", "POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(completeColumns());

        assertThat(resolver.resolve("position_balance")).isEqualTo(new TableDescriptor(
                "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE"));
        assertThat(resolver.resolve("position_balance").hasAuditHistory()).isTrue();
    }

    @Test
    void cachesVerifiedMetadataForRepeatedRequests() {
        when(metadataDao.findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("POSITION_BALANCE", "POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(completeColumns());

        resolver.resolve("POSITION_BALANCE");
        resolver.resolve("position_balance");

        verify(metadataDao, times(1))
                .findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD");
        verify(metadataDao, times(1))
                .findColumnsByTable("POSITION_BALANCE", "POSITION_BALANCE_AUD");
    }

    @Test
    void rejectsAuditNameAndMissingRequiredColumns() {
        assertCode("POSITION_BALANCE_AUD", ApiOutcomeCode.AUDIT_TABLE_NOT_ACCEPTED);

        when(metadataDao.findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("POSITION_BALANCE", "POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(Map.of(
                        "POSITION_BALANCE", Set.of("VERSION"),
                        "POSITION_BALANCE_AUD", Set.of("ID", "REV", "REVTYPE")));
        assertCode("POSITION_BALANCE", ApiOutcomeCode.MISSING_REQUIRED_COLUMN);
    }

    @Test
    void resolvesSourceWithoutAuditTableAndDoesNotCacheMissingAuditMetadata() {
        when(metadataDao.findExistingTables("PMC_CLIENT_PRODUCT_LIMIT",
                "PMC_CLIENT_PRODUCT_LIMIT_AUD"))
                .thenReturn(Set.of("PMC_CLIENT_PRODUCT_LIMIT"))
                .thenReturn(Set.of("PMC_CLIENT_PRODUCT_LIMIT",
                        "PMC_CLIENT_PRODUCT_LIMIT_AUD"));
        when(metadataDao.findColumnsByTable("PMC_CLIENT_PRODUCT_LIMIT",
                "PMC_CLIENT_PRODUCT_LIMIT_AUD"))
                .thenReturn(Map.of("PMC_CLIENT_PRODUCT_LIMIT", Set.of("ID")))
                .thenReturn(Map.of(
                        "PMC_CLIENT_PRODUCT_LIMIT", Set.of("ID"),
                        "PMC_CLIENT_PRODUCT_LIMIT_AUD", Set.of("ID", "REV", "REVTYPE")));

        TableDescriptor sourceOnly = resolver.resolve("PMC_CLIENT_PRODUCT_LIMIT");
        TableDescriptor audited = resolver.resolve("PMC_CLIENT_PRODUCT_LIMIT");

        assertThat(sourceOnly).isEqualTo(new TableDescriptor(
                "PMC_CLIENT_PRODUCT_LIMIT", null, "ID", null, null));
        assertThat(sourceOnly.hasAuditHistory()).isFalse();
        assertThat(audited.hasAuditHistory()).isTrue();
        verify(metadataDao, times(2)).findExistingTables(
                "PMC_CLIENT_PRODUCT_LIMIT", "PMC_CLIENT_PRODUCT_LIMIT_AUD");
    }

    @Test
    void rejectsAnExistingAuditTableWithMissingRevisionMetadata() {
        when(metadataDao.findExistingTables("PMC_CLIENT_PRODUCT_LIMIT",
                "PMC_CLIENT_PRODUCT_LIMIT_AUD"))
                .thenReturn(Set.of(
                        "PMC_CLIENT_PRODUCT_LIMIT", "PMC_CLIENT_PRODUCT_LIMIT_AUD"));
        when(metadataDao.findColumnsByTable("PMC_CLIENT_PRODUCT_LIMIT",
                "PMC_CLIENT_PRODUCT_LIMIT_AUD"))
                .thenReturn(Map.of(
                        "PMC_CLIENT_PRODUCT_LIMIT", Set.of("ID"),
                        "PMC_CLIENT_PRODUCT_LIMIT_AUD", Set.of("ID", "REVTYPE")));

        assertCode("PMC_CLIENT_PRODUCT_LIMIT", ApiOutcomeCode.MISSING_REQUIRED_COLUMN);
    }

    @Test
    void detectsMissingSourceTable() {
        when(metadataDao.findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("POSITION_BALANCE", "POSITION_BALANCE_AUD"));
        when(metadataDao.findColumnsByTable("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(completeColumns());

        assertThat(resolver.resolve("POSITION_BALANCE")).isNotNull();

        TableDescriptorResolver missingSourceResolver = new TableDescriptorResolver(
                metadataDao, properties, new OracleIdentifierValidator());
        when(metadataDao.findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .thenReturn(Set.of("POSITION_BALANCE_AUD"));
        assertThatThrownBy(() -> missingSourceResolver.resolve("POSITION_BALANCE"))
                .isInstanceOfSatisfying(AuditApiException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ApiOutcomeCode.TABLE_PAIR_NOT_FOUND));
    }

    private Map<String, Set<String>> completeColumns() {
        return Map.of(
                "POSITION_BALANCE", Set.of("ID"),
                "POSITION_BALANCE_AUD", Set.of("ID", "REV", "REVTYPE"));
    }

    private void assertCode(String table, ApiOutcomeCode code) {
        assertThatThrownBy(() -> resolver.resolve(table))
                .isInstanceOfSatisfying(AuditApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
