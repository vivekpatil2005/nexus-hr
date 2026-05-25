package com.nexushr.payroll.repository;

import com.nexushr.payroll.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByPeriodMonthAndPeriodYear(int periodMonth, int periodYear);
    boolean existsByPeriodMonthAndPeriodYear(int periodMonth, int periodYear);
}
