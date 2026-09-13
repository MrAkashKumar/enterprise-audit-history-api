package com.akash.auditapi.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class TableMetadataDao {
    private static final String FIND_TABLES_SQL =
            "select table_name from user_tables where table_name in (?, ?)";
    private static final String FIND_COLUMNS_SQL =
            "select table_name, column_name from user_tab_columns where table_name in (?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
}
