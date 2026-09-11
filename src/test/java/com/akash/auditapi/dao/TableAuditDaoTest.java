package com.akash.auditapi.dao;

import com.akash.auditapi.model.TableDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
    void countsAndPaginatesDistinctIds() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(2L);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq(10L), eq(10)))
                .thenReturn(List.of(1002, 1003));

        assertThat(dao.countDistinctIds(table)).isEqualTo(2);
        assertThat(dao.findPageIds(table, 1, 10)).containsExactly(1002, 1003);
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
