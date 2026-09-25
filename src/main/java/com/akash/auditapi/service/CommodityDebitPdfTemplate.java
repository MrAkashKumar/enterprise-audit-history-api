package com.akash.auditapi.service;

import com.akash.auditapi.dto.response.CommodityDebitReportDto;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Renders an A4 portrait commodity debit advice using a clean SWIFT MT606-inspired layout.
 * Only presentation rules live here; every displayed value comes from the report DTO.
 */
@Component
public class CommodityDebitPdfTemplate {
    private static final DeviceRgb HEADER_BACKGROUND = new DeviceRgb(230, 235, 240);
    private static final SolidBorder SECTION_BORDER = new SolidBorder(ColorConstants.DARK_GRAY, 0.8f);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void render(Document document, CommodityDebitReportDto data) {
        Style bold = new Style().setBold();
        FontProvider fontProvider = new FontProvider();
        fontProvider.addStandardPdfFonts();
        document.setFontProvider(fontProvider);
        document.setFontFamily(StandardFonts.COURIER).setFontSize(9);

        document.add(header(bold));
        document.add(referenceBlock(data, bold));
        document.add(routingBlock(data, bold));
        document.add(messageBlock(data, bold));
        document.add(new Paragraph("E N D   M E S S A G E")
                .addStyle(bold).setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(SECTION_BORDER).setBorderBottom(SECTION_BORDER)
                .setMarginTop(8).setMarginBottom(3));
        document.add(new Paragraph("Printed on: " + timestamp(data.printedOn()))
                .setTextAlignment(TextAlignment.RIGHT).setMargin(0));
    }

    private Table header(Style bold) {
        Table table = table(60, 40).setBorderBottom(SECTION_BORDER);
        table.addCell(cell("* INCOMING *", bold).setFontSize(13));
        table.addCell(cell("CUSTOMER PRINT OUT", bold)
                .setFontSize(12).setTextAlignment(TextAlignment.RIGHT));
        return table;
    }

    private Table referenceBlock(CommodityDebitReportDto data, Style bold) {
        Table table = table(24, 76).setMarginTop(8).setMarginBottom(8);
        addPair(table, "REF:", data.incomingReference(), bold);
        addPair(table, "TRN:", data.transactionReference(), bold);
        return table;
    }

    private Table routingBlock(CommodityDebitReportDto data, Style bold) {
        Table table = table(24, 76)
                .setBorderTop(SECTION_BORDER).setBorderBottom(SECTION_BORDER)
                .setPaddingTop(5).setPaddingBottom(5);
        addPair(table, "MT:", data.messageType(), bold);
        addPair(table, "Sender:", joined(data.senderBic(), data.senderName(), data.senderLocation()), bold);
        addPair(table, "Receiver:", joined(data.receiverBic(), data.receiverName(), data.receiverLocation()), bold);
        addPair(table, "Netw. Channel:", data.networkChannel(), bold);
        addPair(table, "Network Ref:", data.networkReference(), bold);
        addPair(table, "Data Owner:", data.dataOwner(), bold);
        addPair(table, "Phase/Action:", data.phaseAction(), bold);
        addPair(table, "MUR:", data.mur(), bold);
        return table;
    }

    private Table messageBlock(CommodityDebitReportDto data, Style bold) {
        Table table = table(12, 55, 33).setMarginTop(8).setBorder(SECTION_BORDER);
        table.addHeaderCell(swiftCell("MT606", bold).setBackgroundColor(HEADER_BACKGROUND));
        table.addHeaderCell(swiftCell("Commodity Debit Advice", bold).setBackgroundColor(HEADER_BACKGROUND));
        table.addHeaderCell(swiftCell(text(data.messageType()), bold).setBackgroundColor(HEADER_BACKGROUND));
        addSwiftRow(table, ":20:", "Transaction Reference Number", data.transactionReference(), bold);
        addSwiftRow(table, ":21:", "Related Reference", data.relatedReference(), bold);
        addSwiftRow(table, ":26C:", "Identification of the Commodity and Delivery Details",
                joined("Delivery Location: " + text(data.deliveryLocation()),
                        "Allocation: " + text(data.allocation()),
                        "Type: " + text(data.commodityType())), bold);
        addSwiftRow(table, ":25:", "Further Account Identification", data.accountIdentification(), bold);
        addSwiftRow(table, ":30:", "Value Date/Date", date(data.valueDate()), bold);
        addSwiftRow(table, ":32F:", "Quantity of the Commodity / Unit - Amount",
                joined("Unit: " + text(data.commodityUnit()),
                        "Amount: " + amount(data.commodityAmount())), bold);
        addSwiftRow(table, ":87A:", "Receiver of the Commodity / Party Identifier",
                joined(data.commodityReceiverIdentifier(), data.commodityReceiverName(),
                        data.commodityReceiverAddress()), bold);
        addSwiftRow(table, ":88D:", "Beneficiary of the Commodity / Name and Address",
                joined(data.beneficiaryName(), data.beneficiaryAddress()), bold);
        return table;
    }

    private void addPair(Table table, String label, Object value, Style bold) {
        table.addCell(cell(label, bold).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(cell(text(value), null));
    }

    private void addSwiftRow(Table table, String field, String description, String value, Style bold) {
        table.addCell(swiftCell(field, bold));
        table.addCell(swiftCell(description, null));
        table.addCell(swiftCell(text(value), bold));
    }

    private Table table(float... widths) {
        return new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
    }

    private Cell cell(String value, Style style) {
        Paragraph paragraph = new Paragraph(value).setMargin(0);
        Optional.ofNullable(style).ifPresent(paragraph::addStyle);
        return new Cell().add(paragraph).setBorder(Border.NO_BORDER).setPadding(2);
    }

    private Cell swiftCell(String value, Style style) {
        return cell(value, style).setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.4f))
                .setPadding(4);
    }

    private String text(Object value) {
        return Objects.toString(value, "");
    }

    private String joined(String... values) {
        return Arrays.stream(values).map(this::text).collect(Collectors.joining("\n"));
    }

    private String date(LocalDate value) {
        return Optional.ofNullable(value).map(DATE_FORMAT::format).orElse("");
    }

    private String timestamp(LocalDateTime value) {
        return Optional.ofNullable(value).map(TIMESTAMP_FORMAT::format).orElse("");
    }

    private String amount(BigDecimal value) {
        return Optional.ofNullable(value)
                .map(number -> NumberFormat.getNumberInstance(Locale.US).format(number))
                .orElse("");
    }
}
