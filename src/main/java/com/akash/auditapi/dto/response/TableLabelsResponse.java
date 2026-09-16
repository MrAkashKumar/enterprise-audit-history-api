package com.akash.auditapi.dto.response;

import java.util.List;

/**
 * Returns the alphabetically ordered public labels for discovered source tables.
 * The table-list endpoint exposes this DTO without pagination.
 */
public final class TableLabelsResponse {
    private final List<String> tableLabels;

    public TableLabelsResponse(List<String> tableLabels) {
        this.tableLabels = List.copyOf(tableLabels);
    }

    public List<String> getTableLabels() {
        return tableLabels;
    }
}
