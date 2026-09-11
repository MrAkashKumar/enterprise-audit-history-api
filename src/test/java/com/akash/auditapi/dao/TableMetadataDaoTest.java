package com.akash.auditapi.dao;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableMetadataDaoTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TableMetadataDao dao = new TableMetadataDao(jdbcTemplate);

    @Test
    void returnsExistingTablesAndColumnPresence() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class),
                eq("PMC_POSITION_BALANCE"), eq("PMC_POSITION_BALANCE_AUD")))
                .thenReturn(List.of("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class),
                eq("PMC_POSITION_BALANCE_AUD"), eq("REV"))).thenReturn(1);

        assertThat(dao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .containsExactlyInAnyOrder("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD");
        assertThat(dao.columnExists("PMC_POSITION_BALANCE_AUD", "REV")).isTrue();
    }

    @Test
    void treatsNullColumnCountAsMissing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                .thenReturn(null);
        assertThat(dao.columnExists("T", "C")).isFalse();
    }
}
