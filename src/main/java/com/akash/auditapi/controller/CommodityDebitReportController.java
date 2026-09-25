package com.akash.auditapi.controller;

import com.akash.auditapi.service.CommodityDebitReportService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.akash.auditapi.constants.ApiPaths.COMMODITY_DEBIT_PDF;
import static com.akash.auditapi.constants.ApiPaths.V1;

/**
 * Exposes the database-backed commodity debit report as a downloadable PDF.
 * Successful responses are binary; errors continue to use the shared JSON error envelope.
 */
@RestController
@RequestMapping(V1)
public class CommodityDebitReportController {
    static final String DOWNLOAD_FILENAME = "community.pdf";

    private final CommodityDebitReportService service;

    public CommodityDebitReportController(CommodityDebitReportService service) {
        this.service = service;
    }

    @GetMapping(value = COMMODITY_DEBIT_PDF, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] pdf = service.generate(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(DOWNLOAD_FILENAME)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }
}
