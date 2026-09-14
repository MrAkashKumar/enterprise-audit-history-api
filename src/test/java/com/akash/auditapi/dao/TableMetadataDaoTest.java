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
                eq("POSITION_BALANCE"), eq("POSITION_BALANCE_AUD")))
                .thenReturn(List.of("POSITION_BALANCE", "POSITION_BALANCE_AUD"));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("TABLE_NAME"))
                .thenReturn("POSITION_BALANCE", "POSITION_BALANCE_AUD",
                        "POSITION_BALANCE_AUD");
        when(resultSet.getString("COLUMN_NAME")).thenReturn("ID", "REV", "REVTYPE");
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            handler.processRow(resultSet);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class),
                eq("POSITION_BALANCE"), eq("POSITION_BALANCE_AUD"));

        assertThat(dao.findExistingTables("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .containsExactlyInAnyOrder("POSITION_BALANCE", "POSITION_BALANCE_AUD");
        assertThat(dao.findColumnsByTable("POSITION_BALANCE", "POSITION_BALANCE_AUD"))
                .containsEntry("POSITION_BALANCE", java.util.Set.of("ID"))
                .containsEntry("POSITION_BALANCE_AUD", java.util.Set.of("REV", "REVTYPE"));
    }
}
