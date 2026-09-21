package com.akash.auditapi.dao;

import com.akash.auditapi.dto.ApprovalTableDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalSqlBuilderTest {
    @Test
    void selectsOnlyTheRequiredApprovalColumns() {
        String sql = new ApprovalSqlBuilder().rows(new ApprovalTableDescriptor(
                "PMC_CLIENT_APPROVAL", "ID", "MAKER_USERNAME", "CHECKER_USERNAME"));

        assertThat(sql).isEqualTo("select ID, MAKER_USERNAME, CHECKER_USERNAME "
                + "from PMC_CLIENT_APPROVAL where ID in (:ids) order by ID");
    }
}
