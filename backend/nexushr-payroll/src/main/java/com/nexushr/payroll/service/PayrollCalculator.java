package com.nexushr.payroll.service;

import com.nexushr.payroll.entity.Payslip;
import com.nexushr.payroll.entity.SalaryStructure;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class PayrollCalculator {

    private static final BigDecimal PF_RATE = new BigDecimal("0.12");
    private static final BigDecimal ESI_EMPLOYEE_RATE = new BigDecimal("0.0075");
    private static final BigDecimal ESI_EMPLOYER_RATE = new BigDecimal("0.0325");
    private static final BigDecimal ESI_THRESHOLD = new BigDecimal("21000.00");
    private static final BigDecimal PROFESSIONAL_TAX = new BigDecimal("200.00");
    private static final BigDecimal STANDARD_DEDUCTION = new BigDecimal("75000.00");
    private static final BigDecimal CESS_RATE = new BigDecimal("0.04");

    public Payslip calculate(SalaryStructure structure, int month, int year, BigDecimal lossOfPayDays) {
        int workingDays = LocalDate.of(year, month, 1).lengthOfMonth();
        BigDecimal workingDaysBd = BigDecimal.valueOf(workingDays);
        BigDecimal presentDaysBd = workingDaysBd.subtract(lossOfPayDays).max(BigDecimal.ZERO);
        int presentDays = presentDaysBd.setScale(0, RoundingMode.HALF_UP).intValue();

        BigDecimal prorationFactor = workingDaysBd.compareTo(BigDecimal.ZERO) > 0
                ? presentDaysBd.divide(workingDaysBd, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Pro-rate earnings
        BigDecimal basic = structure.getBasic().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hra = structure.getHra().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal da = structure.getDa().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal specialAllowance = structure.getSpecialAllowance().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyance = structure.getConveyance().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal medical = structure.getMedical().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal lta = structure.getLta().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherEarnings = structure.getOtherAllowances().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);

        BigDecimal gross = basic.add(hra).add(da).add(specialAllowance)
                .add(conveyance).add(medical).add(lta).add(otherEarnings)
                .setScale(2, RoundingMode.HALF_UP);

        // Deductions
        BigDecimal pfEmployee = basic.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pfEmployer = basic.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP);

        // ESI is applicable if Gross monthly salary is <= 21,000 INR
        BigDecimal esiEmployee = BigDecimal.ZERO;
        BigDecimal esiEmployer = BigDecimal.ZERO;
        if (gross.compareTo(ESI_THRESHOLD) <= 0 && gross.compareTo(BigDecimal.ZERO) > 0) {
            esiEmployee = gross.multiply(ESI_EMPLOYEE_RATE).setScale(2, RoundingMode.HALF_UP);
            esiEmployer = gross.multiply(ESI_EMPLOYER_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal pt = gross.compareTo(BigDecimal.ZERO) > 0 ? PROFESSIONAL_TAX : BigDecimal.ZERO;

        // TDS slab calculation based on annual projection
        BigDecimal annualGross = gross.multiply(BigDecimal.valueOf(12));
        BigDecimal taxableIncome = annualGross.subtract(STANDARD_DEDUCTION).max(BigDecimal.ZERO);
        BigDecimal annualTds = calculateAnnualTds(taxableIncome);
        BigDecimal tds = annualTds.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal otherDeductions = BigDecimal.ZERO;
        BigDecimal totalDeductions = pfEmployee.add(esiEmployee).add(pt).add(tds).add(otherDeductions)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSalary = gross.subtract(totalDeductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Deductions breakdown JSON
        Map<String, Object> deductionsBreakdown = new HashMap<>();
        deductionsBreakdown.put("pf_employee", pfEmployee);
        deductionsBreakdown.put("pf_employer", pfEmployer);
        deductionsBreakdown.put("esi_employee", esiEmployee);
        deductionsBreakdown.put("esi_employer", esiEmployer);
        deductionsBreakdown.put("professional_tax", pt);
        deductionsBreakdown.put("tds", tds);
        deductionsBreakdown.put("other_deductions", otherDeductions);

        return Payslip.builder()
                .employee(structure.getEmployee())
                .empCode(structure.getEmployee().getEmpCode())
                .employeeName(structure.getEmployee().getFullName())
                .department(structure.getEmployee().getDepartment() != null ? structure.getEmployee().getDepartment().getName() : null)
                .designation(structure.getEmployee().getDesignation())
                .basic(basic)
                .hra(hra)
                .da(da)
                .specialAllowance(specialAllowance)
                .otherEarnings(conveyance.add(medical).add(lta).add(otherEarnings).setScale(2, RoundingMode.HALF_UP))
                .gross(gross)
                .pfEmployee(pfEmployee)
                .pfEmployer(pfEmployer)
                .esiEmployee(esiEmployee)
                .esiEmployer(esiEmployer)
                .professionalTax(pt)
                .tds(tds)
                .otherDeductions(otherDeductions)
                .totalDeductions(totalDeductions)
                .netSalary(netSalary)
                .deductionsJson(deductionsBreakdown)
                .workingDays(workingDays)
                .presentDays(presentDays)
                .lossOfPayDays(lossOfPayDays)
                .build();
    }

    private BigDecimal calculateAnnualTds(BigDecimal annualTaxableIncome) {
        if (annualTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // New Tax Regime Slabs for FY 2025-26:
        // - Up to 4,00,000: Nil
        // - 4,00,001 to 8,00,000: 5%
        // - 8,00,001 to 12,00,000: 10%
        // - 12,00,001 to 16,00,000: 15%
        // - 16,00,001 to 20,00,000: 20%
        // - Above 20,00,000: 30%
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal income = annualTaxableIncome;

        BigDecimal s1Limit = new BigDecimal("400000");
        BigDecimal s2Limit = new BigDecimal("800000");
        BigDecimal s3Limit = new BigDecimal("1200000");
        BigDecimal s4Limit = new BigDecimal("1600000");
        BigDecimal s5Limit = new BigDecimal("2000000");

        if (income.compareTo(s1Limit) <= 0) {
            return BigDecimal.ZERO;
        }

        // Slab 2: 4L to 8L (5%)
        if (income.compareTo(s2Limit) > 0) {
            tax = tax.add(s2Limit.subtract(s1Limit).multiply(new BigDecimal("0.05")));
        } else {
            tax = tax.add(income.subtract(s1Limit).multiply(new BigDecimal("0.05")));
            return applyRebateAndCess(tax, annualTaxableIncome);
        }

        // Slab 3: 8L to 12L (10%)
        if (income.compareTo(s3Limit) > 0) {
            tax = tax.add(s3Limit.subtract(s2Limit).multiply(new BigDecimal("0.10")));
        } else {
            tax = tax.add(income.subtract(s2Limit).multiply(new BigDecimal("0.10")));
            return applyRebateAndCess(tax, annualTaxableIncome);
        }

        // Slab 4: 12L to 16L (15%)
        if (income.compareTo(s4Limit) > 0) {
            tax = tax.add(s4Limit.subtract(s3Limit).multiply(new BigDecimal("0.15")));
        } else {
            tax = tax.add(income.subtract(s3Limit).multiply(new BigDecimal("0.15")));
            return applyRebateAndCess(tax, annualTaxableIncome);
        }

        // Slab 5: 16L to 20L (20%)
        if (income.compareTo(s5Limit) > 0) {
            tax = tax.add(s5Limit.subtract(s4Limit).multiply(new BigDecimal("0.20")));
            // Slab 6: Above 20L (30%)
            tax = tax.add(income.subtract(s5Limit).multiply(new BigDecimal("0.30")));
        } else {
            tax = tax.add(income.subtract(s4Limit).multiply(new BigDecimal("0.20")));
        }

        return applyRebateAndCess(tax, annualTaxableIncome);
    }

    private BigDecimal applyRebateAndCess(BigDecimal tax, BigDecimal taxableIncome) {
        // Section 87A rebate for New Tax Regime:
        // Rebate is applicable if taxable income <= 7,00,000 INR (meaning tax is fully rebated up to 20,000 INR)
        // Wait, standard 87A rebate limit in FY 2024-25 / 2025-26 under new regime is 7 Lakhs.
        if (taxableIncome.compareTo(new BigDecimal("700000")) <= 0) {
            return BigDecimal.ZERO;
        }

        // Add 4% Health and Education Cess
        BigDecimal cess = tax.multiply(CESS_RATE);
        return tax.add(cess).setScale(2, RoundingMode.HALF_UP);
    }
}
