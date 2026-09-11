package com.akash.auditapi.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public record AuditRevisionResponse(
        int sequenceNumber,
        Object revision,
        int revisionTypeCode,
        RevisionOperation operation,
        @JsonIgnore
        Map<String, Object> data
) {
    /** Flattens every physical audit-table column into this history JSON object. */
    @JsonAnyGetter
    public Map<String, Object> databaseColumns() {
        return data;
    }
}
