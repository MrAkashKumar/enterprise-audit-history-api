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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalTableResolverTest {
    private static final List<String> SUFFIXES =
            List.of("_APPROVAL_REQUEST", "_APPROVAL");
    private final TableMetadataDao metadataDao = mock(TableMetadataDao.class);
    private final ApprovalProperties properties = new ApprovalProperties(
            null, null, null, null, Map.of());
    private final ApprovalTableResolver resolver = newResolver(properties);

    @Test
    void resolvesAndCachesAConventionalApprovalTable() {
        when(metadataDao.findApprovalTables(SUFFIXES))
                .thenReturn(List.of("PMC_CLIENT_APPROVAL_REQUEST"));
        completeColumns("PMC_CLIENT_APPROVAL_REQUEST");

        ApprovalTableDescriptor expected = new ApprovalTableDescriptor(
                "PMC_CLIENT_APPROVAL_REQUEST", "ID", "MAKER_USERNAME", "CHECKER_USERNAME");
        assertThat(resolver.resolve("pmc_client")).contains(expected);
        assertThat(resolver.resolve("PMC_CLIENT")).contains(expected);
        verify(metadataDao, times(1)).findApprovalTables(SUFFIXES);
    }

    @Test
    void resolvesLegacyApprovalSuffix() {
        when(metadataDao.findApprovalTables(SUFFIXES))
                .thenReturn(List.of("PMC_FEE_SCHEDULE_APPROVAL"));
        completeColumns("PMC_FEE_SCHEDULE_APPROVAL");

        assertThat(resolver.resolve("PMC_FEE_SCHEDULE"))
                .map(ApprovalTableDescriptor::tableName)
                .contains("PMC_FEE_SCHEDULE_APPROVAL");
    }

    @Test
    void returnsAndCachesEmptyWhenNoSafeApprovalMatchExists() {
        when(metadataDao.findApprovalTables(SUFFIXES))
                .thenReturn(List.of("PMC_CLIENT_APPROVAL"));

        assertThat(resolver.resolve("PMC_VAULT")).isEmpty();
        assertThat(resolver.resolve("pmc_vault")).isEmpty();
        verify(metadataDao, times(1)).findApprovalTables(SUFFIXES);
        verify(metadataDao, never()).findColumns("PMC_CLIENT_APPROVAL");
    }

    @Test
    void resolvesConfiguredNamingExceptionWithoutDiscovery() {
        ApprovalProperties overridden = new ApprovalProperties(null, null, null, null,
                Map.of("PMC_LOCO_SINGAPORE", "PMC_LOCO_SG_APPROVAL_REQUEST"));
        completeColumns("PMC_LOCO_SG_APPROVAL_REQUEST");

        assertThat(newResolver(overridden).resolve("PMC_LOCO_SINGAPORE"))
                .map(ApprovalTableDescriptor::tableName)
                .contains("PMC_LOCO_SG_APPROVAL_REQUEST");
        verify(metadataDao, never()).findApprovalTables(SUFFIXES);
    }

    @Test
    void rejectsMissingOverrideAndExactAmbiguity() {
        ApprovalProperties overridden = new ApprovalProperties(null, null, null, null,
                Map.of("PMC_LOCO_SINGAPORE", "PMC_LOCO_SG_APPROVAL_REQUEST"));
        when(metadataDao.findColumns("PMC_LOCO_SG_APPROVAL_REQUEST"))
                .thenReturn(Set.of());
        assertThatThrownBy(() -> newResolver(overridden).resolve("PMC_LOCO_SINGAPORE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configured approval table not found");

        when(metadataDao.findApprovalTables(SUFFIXES)).thenReturn(List.of(
                "PMC_CLIENT_APPROVAL_REQUEST", "PMC_CLIENT_APPROVAL"));
        assertThatThrownBy(() -> resolver.resolve("PMC_CLIENT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple approval tables");

    }

    @Test
    void ignoresDiscoveredTableThatDisappearsBeforeColumnLookup() {
        when(metadataDao.findApprovalTables(SUFFIXES))
                .thenReturn(List.of("PMC_CLIENT_APPROVAL"));
        when(metadataDao.findColumns("PMC_CLIENT_APPROVAL"))
                .thenReturn(Set.of());

        assertThat(resolver.resolve("PMC_CLIENT")).isEmpty();
    }

    @Test
    void rejectsEveryMissingRequiredColumn() {
        assertMissingColumn(Set.of("MAKER_USERNAME", "CHECKER_USERNAME"), "ID", "PMC_ONE");
        assertMissingColumn(Set.of("ID", "CHECKER_USERNAME"), "MAKER_USERNAME", "PMC_TWO");
        assertMissingColumn(Set.of("ID", "MAKER_USERNAME"), "CHECKER_USERNAME", "PMC_THREE");
    }

    private ApprovalTableResolver newResolver(ApprovalProperties selectedProperties) {
        return new ApprovalTableResolver(metadataDao, selectedProperties,
                new OracleIdentifierValidator());
    }

    private void completeColumns(String table) {
        when(metadataDao.findColumns(table))
                .thenReturn(Set.of("ID", "MAKER_USERNAME", "CHECKER_USERNAME"));
    }

    private void assertMissingColumn(Set<String> columns, String missing, String source) {
        String approval = source + "_APPROVAL_REQUEST";
        when(metadataDao.findApprovalTables(SUFFIXES)).thenReturn(List.of(approval));
        when(metadataDao.findColumns(approval)).thenReturn(columns);
        assertThatThrownBy(() -> newResolver(properties).resolve(source))
                .isInstanceOf(MissingAuditColumnException.class)
                .hasMessageContaining(missing);
    }
}
