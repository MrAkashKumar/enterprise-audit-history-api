package com.akash.auditapi.resolver;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.dao.TableMetadataDao;
import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.AuditApiException;
import com.akash.auditapi.validation.OracleIdentifierValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditableTableCatalogTest {
    private final TableMetadataDao metadataDao = mock(TableMetadataDao.class);
    private final AuditApiProperties properties = new AuditApiProperties(
            "PMC_", "_AUD", "ID", "REV", "REVTYPE", 200);
    private final AuditableTableCatalog catalog = new AuditableTableCatalog(
            metadataDao, properties, new OracleIdentifierValidator(), new TableLabelFormatter());

    @Test
    void returnsDynamicLabelsInAlphabeticalOrder() {
        discover("PMC_POSITION_BALANCE", "PMC_ACCOUNT_STATEMENT", "PMC_LOCO_SINGAPORE");

        assertThat(catalog.labels())
                .containsExactly("Account-Statement", "Loco-Singapore", "Position-Balance");
    }

    @Test
    void resolvesFormattedPhysicalLegacyAndPreviousSpaceNames() {
        discover("PMC_POSITION_BALANCE");

        assertThat(catalog.resolve("Position-Balance")).isEqualTo("PMC_POSITION_BALANCE");
        assertThat(catalog.resolve("pmc_position_balance")).isEqualTo("PMC_POSITION_BALANCE");
        assertThat(catalog.resolve("POSITION_BALANCE")).isEqualTo("PMC_POSITION_BALANCE");
        assertThat(catalog.resolve("Position Balance")).isEqualTo("PMC_POSITION_BALANCE");
    }

    @Test
    void rejectsAuditUnknownAndUnsafeNamesWithExistingCodes() {
        discover("PMC_POSITION_BALANCE");

        assertCode("PMC_POSITION_BALANCE_AUD", ApiOutcomeCode.AUDIT_TABLE_NOT_ACCEPTED);
        assertCode("BAD;DROP_AUD", ApiOutcomeCode.INVALID_TABLE_NAME);
        assertCode("UNKNOWN", ApiOutcomeCode.TABLE_NOT_ALLOWED);
        assertCode("BAD;DROP TABLE X", ApiOutcomeCode.INVALID_TABLE_NAME);
        assertCode(null, ApiOutcomeCode.INVALID_TABLE_NAME);
    }

    @Test
    void rejectsConflictingLabelsButToleratesDuplicateMetadataRows() {
        discover("PMC_A_B", "PMC_A__B");
        assertThatThrownBy(catalog::labels)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate table label");

        discover("PMC_ACCOUNT", "PMC_ACCOUNT");
        assertThat(catalog.labels()).containsExactly("Account");
    }

    private void discover(String... sourceTables) {
        when(metadataDao.findSourceTables("PMC_", "_AUD"))
                .thenReturn(List.of(sourceTables));
    }

    private void assertCode(String requestedTable, ApiOutcomeCode code) {
        assertThatThrownBy(() -> catalog.resolve(requestedTable))
                .isInstanceOfSatisfying(AuditApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
