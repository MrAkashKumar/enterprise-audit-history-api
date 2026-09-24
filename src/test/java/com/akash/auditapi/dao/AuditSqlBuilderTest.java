package com.akash.auditapi.dao;

import com.akash.auditapi.dto.TableDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSqlBuilderTest {
    private final AuditSqlBuilder builder = new AuditSqlBuilder();
    private final TableDescriptor table = new TableDescriptor(
            "POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");

    @Test
    void buildsNullSafeUnionPaginationAndFullRowQueries() {
        assertThat(builder.countDistinctIds(table))
                .contains("POSITION_BALANCE", "POSITION_BALANCE_AUD", "ID is not null");
        assertThat(builder.pageIds(table))
                .contains("count(*) over () TOTAL_ELEMENTS",
                        "order by ENTITY_ID offset ? rows fetch next ? rows only");
        assertThat(builder.sourceRows(table))
                .isEqualTo("select * from POSITION_BALANCE where ID in (:ids) order by ID, ID");
        assertThat(builder.auditRows(table))
                .isEqualTo("select * from POSITION_BALANCE_AUD where ID in (:ids) order by ID, REV");
    }

    @Test
    void buildsSourceOnlyPaginationWhenAuditTableDoesNotExist() {
        TableDescriptor sourceOnly = new TableDescriptor(
                "PMC_CLIENT_PRODUCT_LIMIT", null, "ID", null, null);

        assertThat(builder.countDistinctIds(sourceOnly))
                .isEqualTo("select count(*) from (select ID ENTITY_ID from "
                        + "PMC_CLIENT_PRODUCT_LIMIT where ID is not null)");
        assertThat(builder.pageIds(sourceOnly))
                .contains("from (select ID ENTITY_ID from PMC_CLIENT_PRODUCT_LIMIT "
                        + "where ID is not null)")
                .doesNotContain("union", "_AUD");
        assertThat(builder.sourceRows(sourceOnly))
                .isEqualTo("select * from PMC_CLIENT_PRODUCT_LIMIT "
                        + "where ID in (:ids) order by ID, ID");
    }
}
