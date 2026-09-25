package com.akash.auditapi.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.function.BiConsumer;

/**
 * Creates an A4 PDF in memory and delegates report-specific layout to a renderer callback.
 * The generic method can be reused by future database-backed report types.
 */
@Service
public class PdfGenerationService {
    public <T> byte[] generate(T data, BiConsumer<Document, T> renderer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(output));
        try (Document document = new Document(pdf, PageSize.A4)) {
            document.setMargins(28, 28, 28, 28);
            renderer.accept(document, data);
        }
        return output.toByteArray();
    }
}
