package com.nexushr.payroll.batch;

import com.nexushr.payroll.entity.PayrollRun;
import com.nexushr.payroll.entity.Payslip;
import com.nexushr.payroll.repository.PayrollRunRepository;
import com.nexushr.payroll.repository.PayslipRepository;
import com.nexushr.payroll.service.PayslipGenerator;
import com.nexushr.payroll.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@StepScope
public class PayrollItemWriter implements ItemWriter<Payslip> {

    private static final Logger log = LoggerFactory.getLogger(PayrollItemWriter.class);

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private PayrollRunRepository payrollRunRepository;

    @Autowired
    private PayslipGenerator payslipGenerator;

    @Autowired
    private StorageService storageService;

    private final Long payrollRunId;

    public PayrollItemWriter(@Value("#{jobParameters['payrollRunId']}") Long payrollRunId) {
        this.payrollRunId = payrollRunId;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends Payslip> chunk) throws Exception {
        log.info("Writing a chunk of {} payslips for payroll run ID: {}", chunk.size(), payrollRunId);

        BigDecimal grossSum = BigDecimal.ZERO;
        BigDecimal dedSum = BigDecimal.ZERO;
        BigDecimal netSum = BigDecimal.ZERO;
        int count = 0;

        for (Payslip payslip : chunk) {
            // 1. Initial save to get ID and ensure entity is persisted
            Payslip savedPayslip = payslipRepository.save(payslip);

            // 2. Generate PDF and upload to storage
            try {
                byte[] pdfBytes = payslipGenerator.generatePdf(savedPayslip);
                String fileName = "payslip_" + payrollRunId + "_" + savedPayslip.getEmployee().getId() + ".pdf";
                String s3Key = storageService.uploadFile(fileName, pdfBytes, "application/pdf");
                
                savedPayslip.setPdfS3Key(s3Key);
                payslipRepository.save(savedPayslip);
            } catch (Exception e) {
                log.error("Failed to generate/upload PDF for employee ID: {}", savedPayslip.getEmployee().getId(), e);
            }

            // 3. Accumulate totals
            grossSum = grossSum.add(savedPayslip.getGross());
            dedSum = dedSum.add(savedPayslip.getTotalDeductions());
            netSum = netSum.add(savedPayslip.getNetSalary());
            count++;
        }

        // 4. Update the PayrollRun entity totals atomically
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found: " + payrollRunId));
        
        run.setTotalGross(run.getTotalGross().add(grossSum));
        run.setTotalDeductions(run.getTotalDeductions().add(dedSum));
        run.setTotalNet(run.getTotalNet().add(netSum));
        run.setEmployeeCount(run.getEmployeeCount() + count);
        
        payrollRunRepository.save(run);
        log.info("Updated payroll run ID: {} totals with gross={}, deductions={}, net={}, count={}",
                payrollRunId, grossSum, dedSum, netSum, count);
    }
}
