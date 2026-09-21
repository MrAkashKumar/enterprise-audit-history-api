package com.akash.auditapi.dao;

import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Loads approval usernames for all IDs on a page in one database round trip.
 * Only the three required columns are materialized.
 */
@Repository
public class ApprovalDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ApprovalSqlBuilder sqlBuilder;

    public ApprovalDao(NamedParameterJdbcTemplate jdbcTemplate, ApprovalSqlBuilder sqlBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
    }

    public List<ApprovalRecord> findByIds(ApprovalTableDescriptor table, List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(sqlBuilder.rows(table),
                new MapSqlParameterSource("ids", ids),
                (resultSet, rowNumber) -> new ApprovalRecord(
                        resultSet.getObject(table.idColumn()),
                        resultSet.getString(table.makerUsernameColumn()),
                        resultSet.getString(table.checkerUsernameColumn())));
    }
}
