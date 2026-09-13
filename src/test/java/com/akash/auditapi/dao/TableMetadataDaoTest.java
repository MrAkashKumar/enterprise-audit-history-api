package com.akash.auditapi.dao;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableMetadataDaoTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TableMetadataDao dao = new TableMetadataDao(jdbcTemplate);

    @Test
    void returnsExistingTablesAndLoadsAllColumnsInOneQuery() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class),
                eq("PMC_POSITION_BALANCE"), eq("PMC_POSITION_BALANCE_AUD")))
                .thenReturn(List.of("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("TABLE_NAME"))
                .thenReturn("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD",
                        "PMC_POSITION_BALANCE_AUD");
        when(resultSet.getString("COLUMN_NAME")).thenReturn("ID", "REV", "REVTYPE");
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class),
                eq("PMC_POSITION_BALANCE"), eq("PMC_POSITION_BALANCE_AUD"));

        assertThat(dao.findExistingTables("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .containsExactlyInAnyOrder("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD");
        assertThat(dao.findColumnsByTable("PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD"))
                .containsEntry("PMC_POSITION_BALANCE", java.util.Set.of("ID"))
                .containsEntry("PMC_POSITION_BALANCE_AUD", java.util.Set.of("REV", "REVTYPE"));
    }
}
