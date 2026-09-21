package com.akash.auditapi.resolver;

import com.akash.auditapi.config.ApprovalProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import com.akash.auditapi.exception.MissingAuditColumnException;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalTableResolverTest {
    private final TableMetadataDao metadataDao = mock(TableMetadataDao.class);
    private final ApprovalProperties properties = new ApprovalProperties(
            null, null, null, null, Map.of());
    private final ApprovalTableResolver resolver = new ApprovalTableResolver(
            metadataDao, properties, new OracleIdentifierValidator());

    @Test
    void resolvesAndCachesAConventionalApprovalTable() {
        List<String> candidates = List.of(
                "PMC_CLIENT_APPROVAL_REQUEST", "PMC_CLIENT_APPROVAL");
        when(metadataDao.findColumnsByTables(candidates)).thenReturn(Map.of(
                "PMC_CLIENT_APPROVAL_REQUEST",
                Set.of("ID", "MAKER_USERNAME", "CHECKER_USERNAME")));

        ApprovalTableDescriptor expected = new ApprovalTableDescriptor(
                "PMC_CLIENT_APPROVAL_REQUEST", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        assertThat(resolver.resolve("pmc_client")).contains(expected);
        assertThat(resolver.resolve("PMC_CLIENT")).contains(expected);
        verify(metadataDao, times(1)).findColumnsByTables(candidates);
    }

    @Test
    void returnsAndCachesEmptyWhenNoApprovalTableExists() {
        List<String> candidates = List.of(
                "PMC_VAULT_APPROVAL_REQUEST", "PMC_VAULT_APPROVAL");
        when(metadataDao.findColumnsByTables(candidates)).thenReturn(Map.of());

        assertThat(resolver.resolve("PMC_VAULT")).isEmpty();
        assertThat(resolver.resolve("pmc_vault")).isEmpty();
        verify(metadataDao, times(1)).findColumnsByTables(candidates);
    }

    @Test
    void resolvesConfiguredNamingException() {
        ApprovalProperties overridden = new ApprovalProperties(null, null, null, null,
                Map.of("PMC_LOCO_SINGAPORE", "PMC_LOCO_SG_APPROVAL_REQUEST"));
        ApprovalTableResolver overriddenResolver = new ApprovalTableResolver(
                metadataDao, overridden, new OracleIdentifierValidator());
        when(metadataDao.findColumnsByTables(List.of("PMC_LOCO_SG_APPROVAL_REQUEST")))
                .thenReturn(Map.of("PMC_LOCO_SG_APPROVAL_REQUEST",
                        Set.of("ID", "MAKER_USERNAME", "CHECKER_USERNAME")));

        assertThat(overriddenResolver.resolve("PMC_LOCO_SINGAPORE"))
                .map(ApprovalTableDescriptor::tableName)
                .contains("PMC_LOCO_SG_APPROVAL_REQUEST");
    }

    @Test
    void rejectsMissingOverrideAndAmbiguousConventions() {
        ApprovalProperties overridden = new ApprovalProperties(null, null, null, null,
                Map.of("PMC_LOCO_SINGAPORE", "PMC_LOCO_SG_APPROVAL_REQUEST"));
        ApprovalTableResolver overriddenResolver = new ApprovalTableResolver(
                metadataDao, overridden, new OracleIdentifierValidator());
        when(metadataDao.findColumnsByTables(List.of("PMC_LOCO_SG_APPROVAL_REQUEST")))
                .thenReturn(Map.of());
        assertThatThrownBy(() -> overriddenResolver.resolve("PMC_LOCO_SINGAPORE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configured approval table not found");

        List<String> candidates = List.of(
                "PMC_CLIENT_APPROVAL_REQUEST", "PMC_CLIENT_APPROVAL");
        when(metadataDao.findColumnsByTables(candidates)).thenReturn(Map.of(
                "PMC_CLIENT_APPROVAL_REQUEST", Set.of("ID"),
                "PMC_CLIENT_APPROVAL", Set.of("ID")));
        assertThatThrownBy(() -> resolver.resolve("PMC_CLIENT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple approval tables");
    }

    @Test
    void rejectsEveryMissingRequiredColumn() {
        assertMissingColumn(Set.of("MAKER_USERNAME", "CHECKER_USERNAME"), "ID", "PMC_ONE");
        assertMissingColumn(Set.of("ID", "CHECKER_USERNAME"), "MAKER_USERNAME", "PMC_TWO");
        assertMissingColumn(Set.of("ID", "MAKER_USERNAME"), "CHECKER_USERNAME", "PMC_THREE");
    }

    private void assertMissingColumn(Set<String> columns, String missing, String source) {
        String approval = source + "_APPROVAL_REQUEST";
        when(metadataDao.findColumnsByTables(List.of(approval, source + "_APPROVAL")))
                .thenReturn(Map.of(approval, columns));
        assertThatThrownBy(() -> resolver.resolve(source))
                .isInstanceOf(MissingAuditColumnException.class)
                .hasMessageContaining(missing);
    }
}
