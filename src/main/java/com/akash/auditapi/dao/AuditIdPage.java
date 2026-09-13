package com.akash.auditapi.dao;

import java.util.List;

/** Internal database result containing one page of entity IDs and the unpaged total. */
public record AuditIdPage(List<Object> ids, long totalElements) {
    public AuditIdPage {
        ids = List.copyOf(ids);
    }
}
