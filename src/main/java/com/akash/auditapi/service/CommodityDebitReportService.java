package com.akash.auditapi.service;

import com.akash.auditapi.dao.CommodityDebitRepository;
import com.akash.auditapi.dto.response.CommodityDebitReportDto;
import com.akash.auditapi.entity.CommodityDebit;
import com.akash.auditapi.exception.CommodityDebitNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads commodity debit data through JPA and coordinates the reusable PDF generator and template.
 * The complete document is generated inside the read-only service operation before HTTP delivery.
 */
@Service
public class CommodityDebitReportService {
    private final CommodityDebitRepository repository;
    private final PdfGenerationService pdfGenerationService;
    private final CommodityDebitPdfTemplate template;

    public CommodityDebitReportService(CommodityDebitRepository repository,
                                       PdfGenerationService pdfGenerationService,
                                       CommodityDebitPdfTemplate template) {
        this.repository = repository;
        this.pdfGenerationService = pdfGenerationService;
        this.template = template;
    }

    @Transactional(readOnly = true)
    public byte[] generate(Long id) {
        CommodityDebit entity = repository.findById(id)
                .orElseThrow(() -> new CommodityDebitNotFoundException(id));
        CommodityDebitReportDto data = CommodityDebitReportDto.from(entity);
        return pdfGenerationService.generate(data, template::render);
    }
}
