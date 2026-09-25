package com.akash.auditapi.service;

import com.akash.auditapi.dto.response.CommodityDebitReportDto;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommodityDebitPdfTemplateTest {
    private final PdfGenerationService generator = new PdfGenerationService();
    private final CommodityDebitPdfTemplate template = new CommodityDebitPdfTemplate();

    @Test
    void generatesAnA4PdfContainingAllDatabaseBackedReportSections() throws Exception {
        byte[] bytes = generator.generate(populatedData(), template::render);

        assertThat(bytes).startsWith("%PDF".getBytes());
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            assertThat(pdf.getFirstPage().getPageSize().getWidth()).isEqualTo(PageSize.A4.getWidth());
            assertThat(pdf.getFirstPage().getPageSize().getHeight()).isEqualTo(PageSize.A4.getHeight());
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertThat(text).contains(
                    "INCOMING", "CUSTOMER PRINT OUT", "REF-10", "TRN-10",
                    "606 Commodity Debit Advice", "SENDERBIC", "Sender Bank", "London",
                    "RECEIVERBIC", "Receiver Bank", "Singapore", "CHANNEL-10", "NETWORK-10",
                    "OWNER", "ProcessingAlerted", "MUR-10", ":20:", ":21:", ":26C:",
                    ":25:", ":30:", ":32F:", ":87A:", ":88D:", "RELATED-10", "UNALL",
                    "GOLD", "ACCOUNT-10", "2026-07-03", "FOZ", "2,770", "PARTYBIC",
                    "Party Name", "Party Address", "Beneficiary", "Beneficiary Address",
                    "E N D   M E S S A G E", "2026-07-03 15:18:47");
        }
    }

    @Test
    void rendersNullableDatabaseColumnsAsBlankText() throws Exception {
        CommodityDebitReportDto emptyData = new CommodityDebitReportDto(
                11L, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        byte[] bytes = generator.generate(emptyData, template::render);

        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertThat(text).contains("Commodity Debit Advice", "Printed on:")
                    .doesNotContain("null");
        }
    }

    private CommodityDebitReportDto populatedData() {
        return new CommodityDebitReportDto(
                10L, "REF-10", "TRN-10", "606 Commodity Debit Advice",
                "SENDERBIC", "Sender Bank", "London", "RECEIVERBIC", "Receiver Bank",
                "Singapore", "CHANNEL-10", "NETWORK-10", "OWNER", "ProcessingAlerted",
                "MUR-10", "RELATED-10", "LONDON", "UNALL", "GOLD", "ACCOUNT-10",
                LocalDate.of(2026, 7, 3), "FOZ", new BigDecimal("2770"), "PARTYBIC",
                "Party Name", "Party Address", "Beneficiary", "Beneficiary Address",
                LocalDateTime.of(2026, 7, 3, 15, 18, 47));
    }
}
