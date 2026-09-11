package com.akash.auditapi.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class TableMetadataDao {
    private static final String FIND_TABLES_SQL =
            "select table_name from user_tables where table_name in (?, ?)";
    private static final String COUNT_COLUMN_SQL =
            "select count(*) from user_tab_columns where table_name = ? and column_name = ?";

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> findExistingTables(String sourceTable, String auditTable) {
        List<String> names = jdbcTemplate.queryForList(
                FIND_TABLES_SQL, String.class, sourceTable, auditTable);
        return new HashSet<>(names);
    }

    public boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(COUNT_COLUMN_SQL, Integer.class, table, column);
        return count != null && count > 0;
    }
}
