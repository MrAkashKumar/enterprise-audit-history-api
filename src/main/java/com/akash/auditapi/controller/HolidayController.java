package com.akash.auditapi.controller;

import com.akash.auditapi.dto.request.HolidayRequest;
import com.akash.auditapi.dto.response.ApiResponse;
import com.akash.auditapi.dto.response.HolidayResponse;
import com.akash.auditapi.dto.response.PageResponse;
import com.akash.auditapi.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.akash.auditapi.constants.ApiPaths.HOLIDAYS;
import static com.akash.auditapi.constants.ApiPaths.RESOURCE_ID;
import static com.akash.auditapi.constants.ApiPaths.V1;
import static com.akash.auditapi.constants.AuditDefaults.DEFAULT_PAGE_NUMBER_TEXT;
import static com.akash.auditapi.constants.AuditDefaults.DEFAULT_PAGE_SIZE_TEXT;
import static com.akash.auditapi.exception.ApiMessages.REQUEST_SUCCESSFUL;

/**
 * Exposes the typed Holiday CRUD endpoints backed by Spring Data JPA.
 * It validates HTTP input and wraps service results in the common API envelope.
 */
@RestController
@RequestMapping(V1 + HOLIDAYS)
public class HolidayController {
    private final HolidayService service;
    public HolidayController(HolidayService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HolidayResponse>>> findAll(
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER_TEXT) int pageNo,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_TEXT) int pageSize) {
        PageResponse<HolidayResponse> data = PageResponse.from(
                service.findAll(pageNo, pageSize), HolidayResponse::from);
        return ResponseEntity.ok(ApiResponse.success(data, REQUEST_SUCCESSFUL));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HolidayResponse>> create(
            @Valid @RequestBody HolidayRequest request) {
        HolidayResponse data = HolidayResponse.from(service.create(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, REQUEST_SUCCESSFUL));
    }

    @PutMapping(RESOURCE_ID)
    public ResponseEntity<ApiResponse<HolidayResponse>> update(
            @PathVariable Long id, @Valid @RequestBody HolidayRequest request) {
        HolidayResponse data = HolidayResponse.from(service.update(id, request));
        return ResponseEntity.ok(ApiResponse.success(data, REQUEST_SUCCESSFUL));
    }

    @DeleteMapping(RESOURCE_ID)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, REQUEST_SUCCESSFUL));
    }
}
