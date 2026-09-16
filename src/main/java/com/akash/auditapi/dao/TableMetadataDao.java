package com.akash.auditapi.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads Oracle dictionary metadata for discoverable tables and required columns.
 * It supports catalog discovery and safe descriptor verification.
 */
@Repository
public class TableMetadataDao {
    private static final String FIND_SOURCE_TABLES_SQL = """
            select table_name
            from user_tables
            where table_name like ? escape '\\'
              and table_name not like ? escape '\\'
              and table_name <> ?
            order by table_name
            """;
    private static final String FIND_TABLES_SQL =
            "select table_name from user_tables where table_name in (?, ?)";
    private static final String FIND_COLUMNS_SQL =
            "select table_name, column_name from user_tab_columns where table_name in (?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findSourceTables(String sourceTablePrefix, String auditSuffix) {
        String sourcePattern = escapeLikeLiteral(sourceTablePrefix) + "%";
        String auditPattern = "%" + escapeLikeLiteral(auditSuffix);
        return jdbcTemplate.queryForList(
                FIND_SOURCE_TABLES_SQL, String.class, sourcePattern, auditPattern,
                sourceTablePrefix);
    }

    public Set<String> findExistingTables(String sourceTable, String auditTable) {
        List<String> names = jdbcTemplate.queryForList(
                FIND_TABLES_SQL, String.class, sourceTable, auditTable);
        return new HashSet<>(names);
    }

    public Map<String, Set<String>> findColumnsByTable(String sourceTable, String auditTable) {
        Map<String, Set<String>> columnsByTable = new HashMap<>();
        jdbcTemplate.query(FIND_COLUMNS_SQL, resultSet -> {
            String table = resultSet.getString("TABLE_NAME");
            String column = resultSet.getString("COLUMN_NAME");
            columnsByTable.computeIfAbsent(table, ignored -> new HashSet<>()).add(column);
        }, sourceTable, auditTable);
        return columnsByTable;
    }

    private String escapeLikeLiteral(String value) {
        return value.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("%", "\\%");
    }
}
