package com.akash.auditapi.dao;

import com.akash.auditapi.model.TableDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSqlBuilderTest {
    private final AuditSqlBuilder builder = new AuditSqlBuilder();
    private final TableDescriptor table = new TableDescriptor(
            "PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");

    @Test
    void buildsNullSafeUnionPaginationAndFullRowQueries() {
        assertThat(builder.countDistinctIds(table))
                .contains("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD", "ID is not null");
        assertThat(builder.pageIds(table))
                .contains("count(*) over () TOTAL_ELEMENTS",
                        "order by ENTITY_ID offset ? rows fetch next ? rows only");
        assertThat(builder.sourceRows(table))
                .isEqualTo("select * from PMC_POSITION_BALANCE where ID in (:ids) order by ID, ID");
        assertThat(builder.auditRows(table))
                .isEqualTo("select * from PMC_POSITION_BALANCE_AUD where ID in (:ids) order by ID, REV");
    }
}
