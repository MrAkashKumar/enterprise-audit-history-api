package com.akash.auditapi.model;

import java.util.List;

public final class SearchResponse {
    private final String sourceTable;
    private final String auditTable;
    private final int pageNo;
    private final int pageSize;
    private final int numberOfElements;
    private final long totalElements;
    private final long totalPages;
    private final boolean hasPrevious;
    private final boolean hasNext;
    private final List<AuditedRowResponse> rows;

    public SearchResponse(String sourceTable, String auditTable, int pageNo, int pageSize,
                          int numberOfElements, long totalElements, long totalPages,
                          boolean hasPrevious, boolean hasNext, List<AuditedRowResponse> rows) {
        this.sourceTable = sourceTable;
        this.auditTable = auditTable;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.numberOfElements = numberOfElements;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
        this.rows = List.copyOf(rows);
    }

    public String getSourceTable() { return sourceTable; }
    public String getAuditTable() { return auditTable; }
    public int getPageNo() { return pageNo; }
    public int getPageSize() { return pageSize; }
    public int getNumberOfElements() { return numberOfElements; }
    public long getTotalElements() { return totalElements; }
    public long getTotalPages() { return totalPages; }
    public boolean isHasPrevious() { return hasPrevious; }
    public boolean isHasNext() { return hasNext; }
    public List<AuditedRowResponse> getRows() { return rows; }
}
