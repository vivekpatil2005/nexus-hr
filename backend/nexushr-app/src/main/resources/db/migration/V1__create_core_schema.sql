-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  V1__create_core_schema.sql                                     ║
-- ║  NexusHR Core Database Schema                                   ║
-- ║  Creates: departments, employees, users, audit_logs             ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ─── Departments ───
CREATE TABLE departments (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    description     VARCHAR(500),
    parent_id       BIGINT          REFERENCES departments(id) ON DELETE SET NULL,
    head_id         BIGINT,         -- References employees.id (added after employees table)
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_departments_parent ON departments(parent_id);
CREATE INDEX idx_departments_active ON departments(active);

-- ─── Employees ───
CREATE TABLE employees (
    id                  BIGSERIAL       PRIMARY KEY,
    emp_code            VARCHAR(20)     NOT NULL UNIQUE,
    first_name          VARCHAR(50)     NOT NULL,
    last_name           VARCHAR(50)     NOT NULL,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    phone               VARCHAR(15),
    department_id       BIGINT          REFERENCES departments(id) ON DELETE SET NULL,
    manager_id          BIGINT          REFERENCES employees(id) ON DELETE SET NULL,
    designation         VARCHAR(100),
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    hire_date           DATE            NOT NULL,
    exit_date           DATE,
    skills              JSONB,
    ctc                 NUMERIC(12,2),
    profile_photo_url   VARCHAR(200),
    address             VARCHAR(200),
    city                VARCHAR(100),
    state               VARCHAR(50),
    date_of_birth       DATE,
    gender              VARCHAR(20),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add FK from departments.head_id to employees
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head FOREIGN KEY (head_id) REFERENCES employees(id) ON DELETE SET NULL;

CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_emp_code ON employees(emp_code);
CREATE INDEX idx_employees_hire_date ON employees(hire_date);

-- Full-text search index on employee names
CREATE INDEX idx_employees_name_search ON employees
    USING GIN (to_tsvector('english', first_name || ' ' || last_name));

-- ─── Users (Authentication) ───
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    employee_id     BIGINT          REFERENCES employees(id) ON DELETE SET NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_employee ON users(employee_id);

-- ─── User Roles ───
CREATE TABLE user_roles (
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ─── Refresh Tokens ───
CREATE TABLE refresh_tokens (
    token       VARCHAR(255)    PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL,
    expiry_date TIMESTAMP       NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,
    family      VARCHAR(255)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_username ON refresh_tokens(username);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);

-- ─── Audit Logs ───
CREATE TABLE audit_logs (
    id          BIGSERIAL       PRIMARY KEY,
    actor       VARCHAR(100)    NOT NULL,
    action      VARCHAR(100)    NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    details     TEXT,
    ip_address  VARCHAR(50),
    timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
