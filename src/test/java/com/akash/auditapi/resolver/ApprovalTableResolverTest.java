package com.akash.auditapi.resolver;

import com.akash.auditapi.config.ApprovalProperties;
import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import com.akash.auditapi.exception.MissingAuditColumnException;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalTableResolverTest {
    private final TableMetadataDao metadataDao = mock(TableMetadataDao.class);
    private final ApprovalProperties properties = new ApprovalProperties(null, null, null);
    private final AuditApiProperties auditProperties = new AuditApiProperties(
            "PMC_", "_AUD", "ID", "REV", "REVTYPE", 200);
    private final ApprovalTableResolver resolver = newResolver(properties);

    @Test
    void resolvesAndCachesAConventionalApprovalTable() {
        completeColumns("PMC_CLIENT_APPROVAL_REQUEST");
        noPair("PMC_CLIENT_APPROVAL");

        ApprovalTableDescriptor expected = new ApprovalTableDescriptor(
                "PMC_CLIENT_APPROVAL_REQUEST", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        assertThat(resolver.resolve("pmc_client")).contains(expected);
        assertThat(resolver.resolve("PMC_CLIENT")).contains(expected);
        verify(metadataDao, times(1)).findColumns("PMC_CLIENT_APPROVAL_REQUEST");
        verify(metadataDao, times(1)).findExistingTables(
                "PMC_CLIENT_APPROVAL", "PMC_CLIENT_APPROVAL_AUD");
    }

    @Test
    void resolvesLegacyApprovalSuffix() {
        noPair("PMC_FEE_SCHEDULE_APPROVAL_REQUEST");
        completeColumns("PMC_FEE_SCHEDULE_APPROVAL");

        assertThat(resolver.resolve("PMC_FEE_SCHEDULE"))
                .map(ApprovalTableDescriptor::tableName)
                .contains("PMC_FEE_SCHEDULE_APPROVAL");
    }

    @Test
    void returnsAndCachesEmptyWhenNoSafeApprovalMatchExists() {
        noPair("PMC_VAULT_APPROVAL_REQUEST");
        noPair("PMC_VAULT_APPROVAL");

        assertThat(resolver.resolve("PMC_VAULT")).isEmpty();
        assertThat(resolver.resolve("pmc_vault")).isEmpty();
        verify(metadataDao, times(1)).findExistingTables(
                "PMC_VAULT_APPROVAL_REQUEST", "PMC_VAULT_APPROVAL_REQUEST_AUD");
        verify(metadataDao, times(1)).findExistingTables(
                "PMC_VAULT_APPROVAL", "PMC_VAULT_APPROVAL_AUD");
    }

    @Test
    void rejectsTwoExactApprovalTablesForOneSource() {
        completeColumns("PMC_CLIENT_APPROVAL_REQUEST");
        completeColumns("PMC_CLIENT_APPROVAL");
        assertThatThrownBy(() -> resolver.resolve("PMC_CLIENT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple approval tables");
    }

    @Test
    void ignoresApprovalTableThatDisappearsDuringMetadataValidation() {
        String requestTable = "PMC_CLIENT_APPROVAL_REQUEST";
        when(metadataDao.findExistingTables(requestTable, requestTable + "_AUD"))
                .thenReturn(Set.of(requestTable, requestTable + "_AUD"));
        when(metadataDao.findColumns(requestTable)).thenReturn(Set.of());
        noPair("PMC_CLIENT_APPROVAL");

        assertThat(resolver.resolve("PMC_CLIENT")).isEmpty();
    }

    @Test
    void rejectsEveryMissingRequiredColumn() {
        assertMissingColumn(Set.of("MAKER_USERNAME", "CHECKER_USERNAME"), "ID", "PMC_ONE");
        assertMissingColumn(Set.of("ID", "CHECKER_USERNAME"), "MAKER_USERNAME", "PMC_TWO");
        assertMissingColumn(Set.of("ID", "MAKER_USERNAME"), "CHECKER_USERNAME", "PMC_THREE");
    }

    @Test
    void doesNotLookForNestedApprovalTables() {
        assertThat(resolver.resolve("PMC_CLIENT_APPROVAL")).isEmpty();
        assertThat(resolver.resolve("PMC_CLIENT_APPROVAL_REQUEST")).isEmpty();
        verify(metadataDao, org.mockito.Mockito.never())
                .findExistingTables(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
    }

    private ApprovalTableResolver newResolver(ApprovalProperties selectedProperties) {
        return new ApprovalTableResolver(metadataDao, selectedProperties, auditProperties,
                new OracleIdentifierValidator());
    }

    private void completeColumns(String table) {
        when(metadataDao.findExistingTables(table, table + "_AUD"))
                .thenReturn(Set.of(table, table + "_AUD"));
        when(metadataDao.findColumns(table))
                .thenReturn(Set.of("ID", "MAKER_USERNAME", "CHECKER_USERNAME"));
    }

    private void noPair(String table) {
        when(metadataDao.findExistingTables(table, table + "_AUD")).thenReturn(Set.of());
    }

    private void assertMissingColumn(Set<String> columns, String missing, String source) {
        String approval = source + "_APPROVAL_REQUEST";
        when(metadataDao.findExistingTables(approval, approval + "_AUD"))
                .thenReturn(Set.of(approval, approval + "_AUD"));
        when(metadataDao.findColumns(approval)).thenReturn(columns);
        noPair(source + "_APPROVAL");
        assertThatThrownBy(() -> newResolver(properties).resolve(source))
                .isInstanceOf(MissingAuditColumnException.class)
                .hasMessageContaining(missing);
    }
}
