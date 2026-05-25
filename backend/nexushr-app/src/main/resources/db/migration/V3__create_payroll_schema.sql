-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  V3__create_payroll_schema.sql                                  ║
-- ║  Payroll processing and payslip tables                          ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ─── Salary Structures ───
CREATE TABLE salary_structures (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    effective_from  DATE            NOT NULL,
    effective_to    DATE,
    basic           NUMERIC(12,2)   NOT NULL,
    hra             NUMERIC(12,2)   NOT NULL DEFAULT 0,
    da              NUMERIC(12,2)   NOT NULL DEFAULT 0,
    special_allowance NUMERIC(12,2) NOT NULL DEFAULT 0,
    conveyance      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    medical         NUMERIC(12,2)   NOT NULL DEFAULT 0,
    lta             NUMERIC(12,2)   NOT NULL DEFAULT 0,
    other_allowances NUMERIC(12,2)  NOT NULL DEFAULT 0,
    gross           NUMERIC(12,2)   NOT NULL,
    ctc             NUMERIC(12,2)   NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_salary_employee_date UNIQUE (employee_id, effective_from)
);

CREATE INDEX idx_salary_employee ON salary_structures(employee_id);
CREATE INDEX idx_salary_active ON salary_structures(active);

-- ─── Payroll Runs ───
CREATE TABLE payroll_runs (
    id              BIGSERIAL       PRIMARY KEY,
    period_month    INTEGER         NOT NULL,    -- 1-12
    period_year     INTEGER         NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',  -- DRAFT, PROCESSING, APPROVED, LOCKED, FAILED
    total_gross     NUMERIC(14,2)   DEFAULT 0,
    total_deductions NUMERIC(14,2)  DEFAULT 0,
    total_net       NUMERIC(14,2)   DEFAULT 0,
    employee_count  INTEGER         DEFAULT 0,
    run_by          BIGINT          REFERENCES users(id),
    approved_by     BIGINT          REFERENCES users(id),
    notes           TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payroll_period UNIQUE (period_month, period_year)
);

-- ─── Payslips ───
CREATE TABLE payslips (
    id              BIGSERIAL       PRIMARY KEY,
    payroll_run_id  BIGINT          NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id),
    emp_code        VARCHAR(20)     NOT NULL,
    employee_name   VARCHAR(100)    NOT NULL,
    department      VARCHAR(100),
    designation     VARCHAR(100),
    -- Earnings
    basic           NUMERIC(12,2)   NOT NULL DEFAULT 0,
    hra             NUMERIC(12,2)   NOT NULL DEFAULT 0,
    da              NUMERIC(12,2)   NOT NULL DEFAULT 0,
    special_allowance NUMERIC(12,2) NOT NULL DEFAULT 0,
    other_earnings  NUMERIC(12,2)   NOT NULL DEFAULT 0,
    gross           NUMERIC(12,2)   NOT NULL DEFAULT 0,
    -- Deductions
    pf_employee     NUMERIC(12,2)   NOT NULL DEFAULT 0,   -- 12% of Basic
    pf_employer     NUMERIC(12,2)   NOT NULL DEFAULT 0,   -- 12% of Basic (employer contribution)
    esi_employee    NUMERIC(12,2)   NOT NULL DEFAULT 0,   -- 0.75% if gross <= 21000
    esi_employer    NUMERIC(12,2)   NOT NULL DEFAULT 0,   -- 3.25% if gross <= 21000
    professional_tax NUMERIC(12,2)  NOT NULL DEFAULT 0,   -- State-specific
    tds             NUMERIC(12,2)   NOT NULL DEFAULT 0,   -- Income tax
    other_deductions NUMERIC(12,2)  NOT NULL DEFAULT 0,
    total_deductions NUMERIC(12,2)  NOT NULL DEFAULT 0,
    -- Net
    net_salary      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    -- Metadata
    deductions_json JSONB,          -- Detailed breakdown
    pdf_s3_key      VARCHAR(500),
    working_days    INTEGER,
    present_days    INTEGER,
    loss_of_pay_days NUMERIC(5,1)   DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payslip_run_employee UNIQUE (payroll_run_id, employee_id)
);

CREATE INDEX idx_payslips_employee ON payslips(employee_id);
CREATE INDEX idx_payslips_run ON payslips(payroll_run_id);
