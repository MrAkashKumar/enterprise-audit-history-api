package com.akash.auditapi.model;

import java.util.List;

public final class TableLabelsResponse {
    private final List<String> tableLabels;

    public TableLabelsResponse(List<String> tableLabels) {
        this.tableLabels = List.copyOf(tableLabels);
    }

    public List<String> getTableLabels() {
        return tableLabels;
    }
}
