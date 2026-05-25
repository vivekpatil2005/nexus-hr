package com.nexushr.payroll.service;

import com.nexushr.payroll.entity.Payslip;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class PayslipGenerator {

    private static final Logger log = LoggerFactory.getLogger(PayslipGenerator.class);

    public byte[] generatePdf(Payslip payslip) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Set document margins
            document.setMargins(36, 36, 36, 36);

            // --- Header ---
            Paragraph companyTitle = new Paragraph("NexusHR Enterprise Solutions")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            Paragraph payslipSub = new Paragraph("Payslip for " + 
                    Month.of(payslip.getPayrollRun().getPeriodMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + 
                    " " + payslip.getPayrollRun().getPeriodYear())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);

            document.add(companyTitle);
            document.add(payslipSub);

            // --- Employee Details Table (2 columns, 4 fields) ---
            Table empTable = new Table(UnitValue.createPercentArray(new float[]{20f, 30f, 20f, 30f}));
            empTable.setWidth(UnitValue.createPercentValue(100));
            empTable.setMarginBottom(15);

            empTable.addCell(createLabelCell("Employee Name"));
            empTable.addCell(createValCell(payslip.getEmployeeName()));
            empTable.addCell(createLabelCell("Employee Code"));
            empTable.addCell(createValCell(payslip.getEmpCode()));

            empTable.addCell(createLabelCell("Department"));
            empTable.addCell(createValCell(payslip.getDepartment() != null ? payslip.getDepartment() : "N/A"));
            empTable.addCell(createLabelCell("Designation"));
            empTable.addCell(createValCell(payslip.getDesignation() != null ? payslip.getDesignation() : "N/A"));

            empTable.addCell(createLabelCell("Working Days"));
            empTable.addCell(createValCell(String.valueOf(payslip.getWorkingDays())));
            empTable.addCell(createLabelCell("LOP Days"));
            empTable.addCell(createValCell(String.valueOf(payslip.getLossOfPayDays())));

            empTable.addCell(createLabelCell("Present Days"));
            empTable.addCell(createValCell(String.valueOf(payslip.getPresentDays())));
            empTable.addCell(createLabelCell("Net Pay Period"));
            empTable.addCell(createValCell(payslip.getPayrollRun().getPeriodMonth() + "/" + payslip.getPayrollRun().getPeriodYear()));

            document.add(empTable);

            // --- Earnings & Deductions Table (2 main sections side-by-side) ---
            Table salaryGrid = new Table(UnitValue.createPercentArray(new float[]{50f, 50f}));
            salaryGrid.setWidth(UnitValue.createPercentValue(100));
            salaryGrid.setMarginBottom(20);

            // Earnings Column
            Table earningsTable = new Table(UnitValue.createPercentArray(new float[]{60f, 40f}));
            earningsTable.setWidth(UnitValue.createPercentValue(100));
            earningsTable.addCell(createHeaderCell("Earnings"));
            earningsTable.addCell(createHeaderCell("Amount (INR)"));

            earningsTable.addCell(createLabelCell("Basic Salary"));
            earningsTable.addCell(createAmountCell(payslip.getBasic()));

            earningsTable.addCell(createLabelCell("HRA"));
            earningsTable.addCell(createAmountCell(payslip.getHra()));

            earningsTable.addCell(createLabelCell("DA"));
            earningsTable.addCell(createAmountCell(payslip.getDa()));

            earningsTable.addCell(createLabelCell("Special Allowance"));
            earningsTable.addCell(createAmountCell(payslip.getSpecialAllowance()));

            earningsTable.addCell(createLabelCell("Other Earnings"));
            earningsTable.addCell(createAmountCell(payslip.getOtherEarnings()));

            // Blank lines to balance table sizes
            earningsTable.addCell(createLabelCell(" "));
            earningsTable.addCell(createLabelCell(" "));
            earningsTable.addCell(createLabelCell(" "));
            earningsTable.addCell(createLabelCell(" "));

            earningsTable.addCell(createHeaderCell("Gross Earnings"));
            earningsTable.addCell(createAmountHeaderCell(payslip.getGross()));

            Cell earningsCell = new Cell().add(earningsTable).setPadding(0);
            salaryGrid.addCell(earningsCell);

            // Deductions Column
            Table deductionsTable = new Table(UnitValue.createPercentArray(new float[]{60f, 40f}));
            deductionsTable.setWidth(UnitValue.createPercentValue(100));
            deductionsTable.addCell(createHeaderCell("Deductions"));
            deductionsTable.addCell(createHeaderCell("Amount (INR)"));

            deductionsTable.addCell(createLabelCell("Employee PF"));
            deductionsTable.addCell(createAmountCell(payslip.getPfEmployee()));

            deductionsTable.addCell(createLabelCell("Employee ESI"));
            deductionsTable.addCell(createAmountCell(payslip.getEsiEmployee()));

            deductionsTable.addCell(createLabelCell("Professional Tax"));
            deductionsTable.addCell(createAmountCell(payslip.getProfessionalTax()));

            deductionsTable.addCell(createLabelCell("TDS (Income Tax)"));
            deductionsTable.addCell(createAmountCell(payslip.getTds()));

            deductionsTable.addCell(createLabelCell("Other Deductions"));
            deductionsTable.addCell(createAmountCell(payslip.getOtherDeductions()));

            // Blank lines to balance table sizes
            deductionsTable.addCell(createLabelCell(" "));
            deductionsTable.addCell(createLabelCell(" "));
            deductionsTable.addCell(createLabelCell(" "));
            deductionsTable.addCell(createLabelCell(" "));

            deductionsTable.addCell(createHeaderCell("Total Deductions"));
            deductionsTable.addCell(createAmountHeaderCell(payslip.getTotalDeductions()));

            Cell deductionsCell = new Cell().add(deductionsTable).setPadding(0);
            salaryGrid.addCell(deductionsCell);

            document.add(salaryGrid);

            // --- Net Pay block ---
            Table netPayTable = new Table(UnitValue.createPercentArray(new float[]{50f, 50f}));
            netPayTable.setWidth(UnitValue.createPercentValue(100));
            netPayTable.setMarginBottom(30);

            Cell netLabelCell = new Cell().add(new Paragraph("NET TAKE-HOME SALARY").setFontSize(12).setBold())
                    .setTextAlignment(TextAlignment.LEFT)
                    .setPadding(8);
            Cell netValCell = new Cell().add(new Paragraph("INR " + payslip.getNetSalary().toString()).setFontSize(14).setBold())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(8);

            netPayTable.addCell(netLabelCell);
            netPayTable.addCell(netValCell);
            document.add(netPayTable);

            // --- Footer ---
            Paragraph footer1 = new Paragraph("This is a computer-generated payslip and does not require a physical signature.")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic();
            Paragraph footer2 = new Paragraph("CONFIDENTIAL - NexusHR Internal Document")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold();

            document.add(footer1);
            document.add(footer2);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate payslip PDF", e);
            throw new RuntimeException("PDF Generation failed", e);
        }
    }

    private Cell createLabelCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(9).setBold())
                .setPadding(4);
    }

    private Cell createValCell(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "").setFontSize(9))
                .setPadding(4);
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(10).setBold())
                .setPadding(6);
    }

    private Cell createAmountCell(BigDecimal amount) {
        return new Cell().add(new Paragraph(amount != null ? amount.toString() : "0.00").setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(4);
    }

    private Cell createAmountHeaderCell(BigDecimal amount) {
        return new Cell().add(new Paragraph(amount != null ? amount.toString() : "0.00").setFontSize(10).setBold())
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6);
    }
}
