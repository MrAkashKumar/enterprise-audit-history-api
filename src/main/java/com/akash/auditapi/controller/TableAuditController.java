package com.akash.auditapi.controller;

import com.akash.auditapi.dto.response.ApiResponse;
import com.akash.auditapi.dto.response.SearchResponse;
import com.akash.auditapi.dto.response.TableLabelsResponse;
import com.akash.auditapi.enums.AuditableTable;
import com.akash.auditapi.service.TableAuditService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.akash.auditapi.constants.ApiPaths.ALL_TABLES;
import static com.akash.auditapi.constants.ApiPaths.TABLE_AUDIT;
import static com.akash.auditapi.constants.ApiPaths.V1;
import static com.akash.auditapi.constants.AuditDefaults.DEFAULT_PAGE_NUMBER_TEXT;
import static com.akash.auditapi.constants.AuditDefaults.DEFAULT_PAGE_SIZE_TEXT;
import static com.akash.auditapi.exception.ApiMessages.REQUEST_SUCCESSFUL;

@RestController
@RequestMapping(path = V1, produces = MediaType.APPLICATION_JSON_VALUE)
public class TableAuditController {
    private final TableAuditService tableAuditService;
    public TableAuditController(TableAuditService tableAuditService) { this.tableAuditService = tableAuditService; }

    @GetMapping(ALL_TABLES)
    public ResponseEntity<ApiResponse<TableLabelsResponse>> allTableLabels() {
        TableLabelsResponse data = new TableLabelsResponse(AuditableTable.labelNames());
        return ResponseEntity.ok(ApiResponse.success(data, REQUEST_SUCCESSFUL));
    }

    @GetMapping(TABLE_AUDIT)
    public ResponseEntity<ApiResponse<SearchResponse>> sourceWithAuditHistory(
            @PathVariable String tableName,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER_TEXT) int pageNo,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_TEXT) int pageSize) {
        SearchResponse response = tableAuditService.sourceWithAuditHistory(tableName, pageNo, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response, REQUEST_SUCCESSFUL));
    }
}
