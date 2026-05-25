-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  V2__create_attendance_leave_schema.sql                         ║
-- ║  Attendance tracking and leave management tables                ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ─── Attendance Records ───
CREATE TABLE attendance_records (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    date            DATE            NOT NULL,
    check_in        TIMESTAMP,
    check_out       TIMESTAMP,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PRESENT',  -- PRESENT, ABSENT, HALF_DAY, ON_LEAVE, HOLIDAY, WEEKEND
    overtime_hours  NUMERIC(4,2)    DEFAULT 0,
    notes           VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attendance_employee_date UNIQUE (employee_id, date)
);

CREATE INDEX idx_attendance_employee ON attendance_records(employee_id);
CREATE INDEX idx_attendance_date ON attendance_records(date);
CREATE INDEX idx_attendance_status ON attendance_records(status);

-- ─── Leave Types ───
CREATE TABLE leave_types (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(50)     NOT NULL UNIQUE,
    description     VARCHAR(200),
    default_days    INTEGER         NOT NULL DEFAULT 0,
    carry_forward   BOOLEAN         NOT NULL DEFAULT FALSE,
    max_carry_days  INTEGER         DEFAULT 0,
    is_paid         BOOLEAN         NOT NULL DEFAULT TRUE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Leave Balances ───
CREATE TABLE leave_balances (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id   BIGINT          NOT NULL REFERENCES leave_types(id),
    year            INTEGER         NOT NULL,
    total_days      NUMERIC(5,1)    NOT NULL DEFAULT 0,
    used_days       NUMERIC(5,1)    NOT NULL DEFAULT 0,
    pending_days    NUMERIC(5,1)    NOT NULL DEFAULT 0,
    version         INTEGER         NOT NULL DEFAULT 0,    -- Optimistic locking
    CONSTRAINT uk_leave_balance UNIQUE (employee_id, leave_type_id, year)
);

CREATE INDEX idx_leave_balance_employee ON leave_balances(employee_id);

-- ─── Leave Requests ───
CREATE TABLE leave_requests (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id   BIGINT          NOT NULL REFERENCES leave_types(id),
    from_date       DATE            NOT NULL,
    to_date         DATE            NOT NULL,
    total_days      NUMERIC(5,1)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',  -- DRAFT, PENDING, APPROVED, REJECTED, CANCELLED
    approver_id     BIGINT          REFERENCES employees(id),
    reason          TEXT,
    rejection_reason TEXT,
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates ON leave_requests(from_date, to_date);
CREATE INDEX idx_leave_requests_approver ON leave_requests(approver_id);

-- ─── Seed Leave Types ───
INSERT INTO leave_types (name, description, default_days, carry_forward, max_carry_days, is_paid) VALUES
('Casual Leave', 'For personal/urgent matters', 12, FALSE, 0, TRUE),
('Sick Leave', 'For illness and medical needs', 12, FALSE, 0, TRUE),
('Earned Leave', 'Accumulated privilege leave', 15, TRUE, 30, TRUE),
('Comp Off', 'Compensatory off for extra work', 0, FALSE, 0, TRUE),
('Maternity Leave', 'Maternity/paternity leave', 180, FALSE, 0, TRUE),
('Loss of Pay', 'Unpaid leave', 0, FALSE, 0, FALSE);
