package com.akash.auditapi.dao;

import java.util.List;

/**
 * Carries one page of distinct entity IDs and the unpaged total count.
 * The audit DAO returns it before loading complete source and history rows.
 */
public record AuditIdPage(List<Object> ids, long totalElements) {
    public AuditIdPage {
        ids = List.copyOf(ids);
    }
}
