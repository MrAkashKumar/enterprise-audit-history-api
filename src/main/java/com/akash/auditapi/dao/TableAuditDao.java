package com.akash.auditapi.dao;

import com.akash.auditapi.model.TableDescriptor;
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

    public long countDistinctIds(TableDescriptor table) {
        Long count = jdbcTemplate.queryForObject(sqlBuilder.countDistinctIds(table), Long.class);
        return count == null ? 0 : count;
    }

    public List<Object> findPageIds(TableDescriptor table, int pageNo, int pageSize) {
        return jdbcTemplate.queryForList(sqlBuilder.pageIds(table), Object.class,
                Math.multiplyExact((long) pageNo, pageSize), pageSize);
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
}
