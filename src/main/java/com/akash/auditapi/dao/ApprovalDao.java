package com.akash.auditapi.dao;

import com.akash.auditapi.dto.ApprovalRecord;
import com.akash.auditapi.dto.ApprovalTableDescriptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Loads approval usernames for all IDs on a page in one database round trip.
 * Only the required ID/checker columns and the optional maker column are materialized.
 */
@Repository
public class ApprovalDao {
    private static final String ROWS_WITH_MAKER_SQL =
            "select %1$s, %2$s, %3$s from %4$s where %1$s in (:ids) order by %1$s";
    private static final String ROWS_WITHOUT_MAKER_SQL =
            "select %1$s, %2$s from %3$s where %1$s in (:ids) order by %1$s";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ApprovalDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApprovalRecord> findByIds(ApprovalTableDescriptor table, List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(rowsSql(table),
                new MapSqlParameterSource("ids", ids),
                (resultSet, rowNumber) -> new ApprovalRecord(
                        resultSet.getObject(table.idColumn()),
                        table.makerUsernameColumnPresent()
                                ? resultSet.getString(table.makerUsernameColumn())
                                : null,
                        resultSet.getString(table.checkerUsernameColumn())));
    }

    private String rowsSql(ApprovalTableDescriptor table) {
        if (table.makerUsernameColumnPresent()) {
            return ROWS_WITH_MAKER_SQL.formatted(table.idColumn(), table.makerUsernameColumn(),
                    table.checkerUsernameColumn(), table.tableName());
        }
        return ROWS_WITHOUT_MAKER_SQL.formatted(table.idColumn(), table.checkerUsernameColumn(),
                table.tableName());
    }
}
