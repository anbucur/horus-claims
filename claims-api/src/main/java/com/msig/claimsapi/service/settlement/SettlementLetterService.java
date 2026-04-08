package com.msig.claimsapi.service.settlement;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.repository.FinancialsRepository;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Financials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementLetterService {

    private final ClaimRepository claimRepository;
    private final FinancialsRepository financialsRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(0, 51, 102);

    @Transactional(readOnly = true)
    public byte[] generateSettlementLetter(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        if (claim.getWorkflowStatus() != WorkflowStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Settlement letter can only be generated for COMPLETED claims. Current status: "
                            + claim.getWorkflowStatus());
        }

        List<Financials> allFinancials = financialsRepository.findByClaimId(claimId);

        BigDecimal totalReserve = allFinancials.stream()
                .filter(f -> f.getStatus() == Financials.TransactionStatus.RESERVE)
                .map(Financials::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalApproved = allFinancials.stream()
                .filter(f -> f.getStatus() == Financials.TransactionStatus.PAYMENT && f.getApprovedAt() != null)
                .map(Financials::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = allFinancials.stream()
                .filter(f -> f.getStatus() == Financials.TransactionStatus.PAYMENT && f.getPaidAt() != null)
                .map(Financials::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            addHeader(document, claim);
            addClaimDetails(document, claim);
            addSettlementBreakdown(document, totalReserve, totalApproved, totalPaid, claim);
            addBankDetailsPlaceholder(document);
            addSignatureBlock(document);

            document.close();
            log.info("Generated settlement letter PDF for claim {}", claimId);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate settlement letter for claim {}", claimId, e);
            throw new RuntimeException("Failed to generate settlement letter PDF", e);
        }
    }

    private void addHeader(Document document, Claim claim) {
        Paragraph header = new Paragraph("HORUS MARINE INSURANCE")
                .setFontSize(20f)
                .setFontColor(HEADER_COLOR)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(header);

        Paragraph subHeader = new Paragraph("Settlement Letter")
                .setFontSize(14f)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(subHeader);

        Paragraph datePara = new Paragraph("Date: " + LocalDate.now().format(DATE_FORMAT))
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(datePara);

        document.add(new Paragraph("\n"));
    }

    private void addClaimDetails(Document document, Claim claim) {
        Paragraph title = new Paragraph("SETTLEMENT DETAILS")
                .setFontSize(12f)
                .setFontColor(HEADER_COLOR)
                .setBold();
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Claim Number:", String.valueOf(claim.getId()));
        addTableRow(table, "Date of Loss:",
                claim.getDateOfLoss() != null ? claim.getDateOfLoss().format(DATE_FORMAT) : "N/A");
        addTableRow(table, "Assured Name:",
                claim.getPolicy() != null ? getAssuredName(claim.getPolicy()) : "N/A");
        addTableRow(table, "Policy Number:",
                claim.getPolicy() != null ? claim.getPolicy().getPolicyNumber() : "N/A");
        addTableRow(table, "Status:", claim.getWorkflowStatus().name());
        addTableRow(table, "Settlement Date:",
                claim.getSettledAt() != null ? claim.getSettledAt().format(DATE_FORMAT) : "N/A");

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSettlementBreakdown(Document document, BigDecimal totalReserve,
                                         BigDecimal totalApproved, BigDecimal totalPaid, Claim claim) {
        Paragraph title = new Paragraph("SETTLEMENT BREAKDOWN")
                .setFontSize(12f)
                .setFontColor(HEADER_COLOR)
                .setBold();
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell headerCell1 = new Cell().add(new Paragraph("Description").setBold())
                .setBackgroundColor(HEADER_COLOR).setFontColor(ColorConstants.WHITE);
        Cell headerCell2 = new Cell().add(new Paragraph("Amount").setBold())
                .setBackgroundColor(HEADER_COLOR).setFontColor(ColorConstants.WHITE);
        table.addHeaderCell(headerCell1);
        table.addHeaderCell(headerCell2);

        String currency = claim.getSettlementCurrency() != null ? claim.getSettlementCurrency() : "USD";

        addBreakdownRow(table, "Total Reserves:", formatAmount(totalReserve, currency));
        addBreakdownRow(table, "Total Approved:", formatAmount(totalApproved, currency));
        addBreakdownRow(table, "Total Paid:", formatAmount(totalPaid, currency));

        Cell labelCell = new Cell().add(new Paragraph("NET SETTLEMENT AMOUNT").setBold())
                .setBackgroundColor(new DeviceRgb(230, 230, 230));
        Cell amountCell = new Cell().add(new Paragraph(formatAmount(claim.getSettlementAmount(), currency)).setBold())
                .setBackgroundColor(new DeviceRgb(230, 230, 230));
        table.addCell(labelCell);
        table.addCell(amountCell);

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addBankDetailsPlaceholder(Document document) {
        Paragraph title = new Paragraph("BANK PAYMENT DETAILS")
                .setFontSize(12f)
                .setFontColor(HEADER_COLOR)
                .setBold();
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Bank Name:", "[To be confirmed]");
        addTableRow(table, "Account Name:", "[To be confirmed]");
        addTableRow(table, "Account Number:", "[To be confirmed]");
        addTableRow(table, "SWIFT / BIC:", "[To be confirmed]");

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSignatureBlock(Document document) {
        document.add(new Paragraph("\n\n"));

        Paragraph note = new Paragraph(
                "This settlement letter is generated automatically by the HORUS Claims Processing Platform. " +
                "Payment will be processed within 5-7 business days of confirmation of bank details.")
                .setFontSize(9f)
                .setFontColor(ColorConstants.GRAY);
        document.add(note);

        document.add(new Paragraph("\n"));

        Paragraph sig = new Paragraph("Authorized Signatory")
                .setFontSize(10f);
        document.add(sig);

        document.add(new Paragraph("_".repeat(40)));
        document.add(new Paragraph("HORUS Insurance"));
        document.add(new Paragraph("Claims Settlement Department"));
    }

    private void addTableRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label))
                .setBorder(Border.NO_BORDER);
        Cell valueCell = new Cell().add(new Paragraph(value))
                .setBorder(Border.NO_BORDER);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addBreakdownRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label))
                .setBorder(Border.NO_BORDER);
        Cell amountCell = new Cell().add(new Paragraph(value))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        table.addCell(labelCell);
        table.addCell(amountCell);
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return currency + " 0.00";
        }
        return currency + " " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String getAssuredName(Policy policy) {
        try {
            java.lang.reflect.Method m = policy.getClass().getMethod("getAssuredName");
            Object result = m.invoke(policy);
            return result != null ? result.toString() : "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }
}
