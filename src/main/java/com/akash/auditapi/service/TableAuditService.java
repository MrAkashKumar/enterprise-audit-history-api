package com.akash.auditapi.service;

import com.akash.auditapi.dto.response.SearchResponse;

import java.util.List;

public interface TableAuditService {
    List<String> findAllTableLabels();

    SearchResponse sourceWithAuditHistory(String tableName, int pageNo, int pageSize);
}
