package com.nexushr.payroll.config;

import com.nexushr.common.enums.PayrollRunStatus;
import com.nexushr.employee.entity.Employee;
import com.nexushr.payroll.batch.PayrollItemProcessor;
import com.nexushr.payroll.batch.PayrollItemWriter;
import com.nexushr.payroll.entity.PayrollRun;
import com.nexushr.payroll.entity.Payslip;
import com.nexushr.payroll.repository.PayrollRunRepository;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
public class PayrollBatchJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PayrollBatchJobConfig.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PayrollRunRepository payrollRunRepository;

    @Bean
    public JpaPagingItemReader<Employee> employeeReader() {
        return new JpaPagingItemReaderBuilder<Employee>()
                .name("employeeReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT e FROM Employee e WHERE e.status IN (" +
                        "com.nexushr.common.enums.EmployeeStatus.ACTIVE, " +
                        "com.nexushr.common.enums.EmployeeStatus.ON_LEAVE, " +
                        "com.nexushr.common.enums.EmployeeStatus.PROBATION, " +
                        "com.nexushr.common.enums.EmployeeStatus.NOTICE_PERIOD)")
                .pageSize(10)
                .build();
    }

    @Bean
    public Step payrollStep(JpaPagingItemReader<Employee> reader,
                           PayrollItemProcessor processor,
                           PayrollItemWriter writer) {
        return new StepBuilder("payrollStep", jobRepository)
                .<Employee, Payslip>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job payrollJob(Step payrollStep) {
        return new JobBuilder("payrollJob", jobRepository)
                .listener(payrollJobListener())
                .start(payrollStep)
                .build();
    }

    @Bean
    public JobExecutionListener payrollJobListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                Long payrollRunId = jobExecution.getJobParameters().getLong("payrollRunId");
                log.info("Starting Spring Batch payroll job for run ID: {}", payrollRunId);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                Long payrollRunId = jobExecution.getJobParameters().getLong("payrollRunId");
                log.info("Finished Spring Batch payroll job for run ID: {} with status: {}", 
                        payrollRunId, jobExecution.getStatus());

                payrollRunRepository.findById(payrollRunId).ifPresent(run -> {
                    run.setCompletedAt(LocalDateTime.now());
                    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                        run.setStatus(PayrollRunStatus.DRAFT);
                        log.info("Payroll run ID: {} processed successfully and set to DRAFT.", payrollRunId);
                    } else {
                        run.setStatus(PayrollRunStatus.FAILED);
                        log.error("Payroll run ID: {} failed during processing.", payrollRunId);
                    }
                    payrollRunRepository.save(run);
                });
            }
        };
    }
}
