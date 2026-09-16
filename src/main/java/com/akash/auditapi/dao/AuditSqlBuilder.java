package com.akash.auditapi.dao;

import com.akash.auditapi.dto.TableDescriptor;
import org.springframework.stereotype.Component;

/**
 * Builds Oracle SQL for paginated IDs and complete source/audit row reads.
 * It accepts only previously validated table descriptors.
 */
@Component
public class AuditSqlBuilder {
    static final String ENTITY_ID_ALIAS = "ENTITY_ID";
    static final String TOTAL_ELEMENTS_ALIAS = "TOTAL_ELEMENTS";
    private static final String COUNT_IDS_SQL = "select count(*) from (%s)";
    private static final String PAGE_IDS_SQL =
            "select %1$s, count(*) over () %2$s from (%3$s) "
                    + "order by %1$s offset ? rows fetch next ? rows only";
    private static final String UNION_IDS_SQL =
            "select %1$s %2$s from %3$s where %1$s is not null "
                    + "union select %1$s %2$s from %4$s where %1$s is not null";
    private static final String ROWS_SQL =
            "select * from %1$s where %2$s in (:ids) order by %2$s, %3$s";

    String countDistinctIds(TableDescriptor table) {
        return COUNT_IDS_SQL.formatted(unionIds(table));
    }

    String pageIds(TableDescriptor table) {
        return PAGE_IDS_SQL.formatted(ENTITY_ID_ALIAS, TOTAL_ELEMENTS_ALIAS, unionIds(table));
    }

    String sourceRows(TableDescriptor table) {
        return rows(table.sourceTable(), table.idColumn(), table.idColumn());
    }

    String auditRows(TableDescriptor table) {
        return rows(table.auditTable(), table.idColumn(), table.auditOrderColumn());
    }

    private String unionIds(TableDescriptor table) {
        return UNION_IDS_SQL.formatted(table.idColumn(), ENTITY_ID_ALIAS,
                table.sourceTable(), table.auditTable());
    }

    private String rows(String table, String idColumn, String orderColumn) {
        return ROWS_SQL.formatted(table, idColumn, orderColumn);
    }
}
