package com.akash.auditapi.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalPropertiesTest {
    @Test
    void appliesDefaultsAndRecognizesApprovalTables() {
        ApprovalProperties properties = new ApprovalProperties(null, null, " ", null, null);

        assertThat(properties.suffixes()).containsExactly("_APPROVAL_REQUEST", "_APPROVAL");
        assertThat(properties.idColumn()).isEqualTo("ID");
        assertThat(properties.makerUsernameColumn()).isEqualTo("MAKER_USERNAME");
        assertThat(properties.checkerUsernameColumn()).isEqualTo("CHECKER_USERNAME");
        assertThat(properties.isApprovalTable("pmc_client_approval_request")).isTrue();
        assertThat(properties.isApprovalTable("PMC_CLIENT")).isFalse();
    }

    @Test
    void normalizesDistinctConfigurationAndOverrides() {
        ApprovalProperties properties = new ApprovalProperties(
                List.of(" _approval ", "_APPROVAL"), "entity_id", "maker", "checker",
                Map.of("pmc_loco_singapore", "pmc_loco_sg_flow"));

        assertThat(properties.suffixes()).containsExactly("_APPROVAL");
        assertThat(properties.idColumn()).isEqualTo("ENTITY_ID");
        assertThat(properties.tableOverrides())
                .containsEntry("PMC_LOCO_SINGAPORE", "PMC_LOCO_SG_FLOW");
        assertThat(properties.isApprovalTable("pmc_loco_sg_flow")).isTrue();
    }

    @Test
    void rejectsUnsafeSuffixColumnsAndOverrides() {
        assertThatThrownBy(() -> new ApprovalProperties(
                Arrays.asList((String) null), null, null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalProperties(
                List.of("BAD"), null, null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalProperties(
                List.of(), "ID;DROP", null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalProperties(
                List.of(), null, null, null, Map.of("BAD NAME", "VALID")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalProperties(
                List.of(), null, null, null, Map.of("VALID", "BAD NAME")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
