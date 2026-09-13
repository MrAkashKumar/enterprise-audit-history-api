package com.akash.auditapi.dao;

import com.akash.auditapi.model.TableDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TableAuditDaoTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final NamedParameterJdbcTemplate namedJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final OracleColumnMapRowMapper rowMapper = mock(OracleColumnMapRowMapper.class);
    private final TableAuditDao dao = new TableAuditDao(
            jdbcTemplate, namedJdbcTemplate, new AuditSqlBuilder(), rowMapper);
    private final TableDescriptor table = new TableDescriptor(
            "PMC_POSITION_BALANCE", "PMC_POSITION_BALANCE_AUD", "ID", "REV", "REVTYPE");

    @Test
    void paginatesDistinctIdsAndReturnsWindowCount() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("ENTITY_ID")).thenReturn(1002, 1003);
        when(resultSet.getLong("TOTAL_ELEMENTS")).thenReturn(2L);
        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                eq(10L), eq(10)))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0), mapper.mapRow(resultSet, 1));
                });

        AuditIdPage result = dao.findIdPage(table, 1, 10);

        assertThat(result.ids()).containsExactly(1002, 1003);
        assertThat(result.totalElements()).isEqualTo(2);
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void fallsBackToCountForAnEmptyPageBeyondAvailableRows() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(2L);
        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                eq(20L), eq(10)))
                .thenReturn(List.of());

        AuditIdPage result = dao.findIdPage(table, 2, 10);

        assertThat(result.ids()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void returnsAllSourceAndAuditColumnsAndSkipsEmptyIdQuery() {
        List<Map<String, Object>> rows = List.of(Map.of(
                "ID", 1002, "REV", 9071, "REVTYPE", 1, "TOTAL_AGGREGATED_QUANTITY", 0));
        when(namedJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), eq(rowMapper)))
                .thenReturn(rows);

        assertThat(dao.findAuditRows(table, List.of(1002))).isEqualTo(rows);
        assertThat(dao.findSourceRows(table, List.of())).isEmpty();
        verify(namedJdbcTemplate, never()).query(
                eq("select * from PMC_POSITION_BALANCE where ID in (:ids) order by ID, ID"),
                any(MapSqlParameterSource.class), eq(rowMapper));
    }
}
