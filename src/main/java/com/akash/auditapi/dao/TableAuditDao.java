package com.akash.auditapi.dao;

import com.akash.auditapi.dto.TableDescriptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TableAuditDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final AuditSqlBuilder sqlBuilder;
    private final OracleColumnMapRowMapper rowMapper;

    public TableAuditDao(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate,
                         AuditSqlBuilder sqlBuilder, OracleColumnMapRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.rowMapper = rowMapper;
    }

    private long countDistinctIds(TableDescriptor table) {
        Long count = jdbcTemplate.queryForObject(sqlBuilder.countDistinctIds(table), Long.class);
        return count == null ? 0 : count;
    }

    public AuditIdPage findIdPage(TableDescriptor table, int pageNo, int pageSize) {
        List<IdWithTotal> page = jdbcTemplate.query(sqlBuilder.pageIds(table),
                (resultSet, rowNumber) -> new IdWithTotal(
                        resultSet.getObject(AuditSqlBuilder.ENTITY_ID_ALIAS),
                        resultSet.getLong(AuditSqlBuilder.TOTAL_ELEMENTS_ALIAS)),
                Math.multiplyExact((long) pageNo, pageSize), pageSize);
        if (!page.isEmpty()) {
            return new AuditIdPage(page.stream().map(IdWithTotal::id).toList(),
                    page.getFirst().totalElements());
        }

        // A page beyond the last row has no window-count value, so retain the
        // previous total-elements behaviour with one fallback count query.
        long totalElements = pageNo == 0 ? 0 : countDistinctIds(table);
        return new AuditIdPage(List.of(), totalElements);
    }

    public List<Map<String, Object>> findSourceRows(TableDescriptor table, List<Object> ids) {
        return findRows(sqlBuilder.sourceRows(table), ids);
    }

    public List<Map<String, Object>> findAuditRows(TableDescriptor table, List<Object> ids) {
        return findRows(sqlBuilder.auditRows(table), ids);
    }

    private List<Map<String, Object>> findRows(String sql, List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return namedJdbcTemplate.query(sql, new MapSqlParameterSource("ids", ids), rowMapper);
    }

    private record IdWithTotal(Object id, long totalElements) {
    }
}
