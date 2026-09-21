package com.akash.auditapi.dao;

import com.akash.auditapi.dto.ApprovalTableDescriptor;
import org.springframework.stereotype.Component;

/**
 * Builds the narrow, ID-bound approval query from verified identifiers.
 * It deliberately avoids selecting proposed changes or other sensitive columns.
 */
@Component
public class ApprovalSqlBuilder {
    private static final String ROWS_SQL =
            "select %1$s, %2$s, %3$s from %4$s where %1$s in (:ids) order by %1$s";

    String rows(ApprovalTableDescriptor table) {
        return ROWS_SQL.formatted(table.idColumn(), table.makerUsernameColumn(),
                table.checkerUsernameColumn(), table.tableName());
    }
}
