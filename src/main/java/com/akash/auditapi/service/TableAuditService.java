package com.akash.auditapi.service;

import com.akash.auditapi.dto.response.SearchResponse;

import java.util.List;

/**
 * Defines the business operations exposed by the generic audit controller.
 * Implementations provide table labels and paginated source-with-history searches.
 */
public interface TableAuditService {
    List<String> findAllTableLabels();

    SearchResponse sourceWithAuditHistory(String tableName, int pageNo, int pageSize);
}
