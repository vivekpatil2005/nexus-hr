-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  V4__create_performance_ai_schema.sql                           ║
-- ║  Performance management and AI/ML tables                        ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ─── Review Cycles ───
CREATE TABLE review_cycles (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    type            VARCHAR(20)     NOT NULL DEFAULT 'ANNUAL',  -- QUARTERLY, HALF_YEARLY, ANNUAL
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING',  -- UPCOMING, ACTIVE, COMPLETED, CANCELLED
    description     TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Goals ───
CREATE TABLE goals (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    review_cycle_id BIGINT          REFERENCES review_cycles(id),
    parent_goal_id  BIGINT          REFERENCES goals(id) ON DELETE SET NULL,
    title           VARCHAR(200)    NOT NULL,
    description     TEXT,
    type            VARCHAR(20)     NOT NULL DEFAULT 'SMART',  -- OKR, SMART
    category        VARCHAR(50),
    target_value    NUMERIC(10,2),
    current_value   NUMERIC(10,2)   DEFAULT 0,
    weight          NUMERIC(5,2)    DEFAULT 1.0,  -- Weight for scoring
    unit            VARCHAR(30),    -- percentage, count, currency, etc.
    start_date      DATE,
    due_date        DATE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED
    progress        INTEGER         DEFAULT 0,  -- 0-100
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_goals_employee ON goals(employee_id);
CREATE INDEX idx_goals_cycle ON goals(review_cycle_id);
CREATE INDEX idx_goals_status ON goals(status);

-- ─── Performance Reviews ───
CREATE TABLE performance_reviews (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    reviewer_id     BIGINT          NOT NULL REFERENCES employees(id),
    cycle_id        BIGINT          NOT NULL REFERENCES review_cycles(id),
    review_type     VARCHAR(20)     NOT NULL DEFAULT 'MANAGER',  -- SELF, MANAGER, PEER
    goal_score      NUMERIC(4,2),    -- 0.00 - 5.00
    competency_score NUMERIC(4,2),
    peer_score      NUMERIC(4,2),
    manager_score   NUMERIC(4,2),
    final_score     NUMERIC(4,2),
    band            VARCHAR(10),     -- A+, A, B+, B, C, D
    strengths       TEXT,
    improvement_areas TEXT,
    manager_comments TEXT,
    employee_comments TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',  -- DRAFT, SUBMITTED, ACKNOWLEDGED
    submitted_at    TIMESTAMP,
    acknowledged_at TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_employee_cycle_type UNIQUE (employee_id, cycle_id, review_type, reviewer_id)
);

CREATE INDEX idx_reviews_employee ON performance_reviews(employee_id);
CREATE INDEX idx_reviews_cycle ON performance_reviews(cycle_id);
CREATE INDEX idx_reviews_reviewer ON performance_reviews(reviewer_id);

-- ─── Peer Feedback ───
CREATE TABLE peer_feedbacks (
    id              BIGSERIAL       PRIMARY KEY,
    review_cycle_id BIGINT          NOT NULL REFERENCES review_cycles(id),
    from_employee_id BIGINT         NOT NULL REFERENCES employees(id),
    to_employee_id  BIGINT          NOT NULL REFERENCES employees(id),
    rating          NUMERIC(3,1),    -- 1.0 - 5.0
    strengths       TEXT,
    improvement_areas TEXT,
    comments        TEXT,
    is_anonymous    BOOLEAN         NOT NULL DEFAULT TRUE,
    submitted_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_peer_feedback UNIQUE (review_cycle_id, from_employee_id, to_employee_id)
);

-- ─── AI Attrition Scores ───
CREATE TABLE ai_attrition_scores (
    id              BIGSERIAL       PRIMARY KEY,
    employee_id     BIGINT          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    score           NUMERIC(5,4)    NOT NULL,  -- 0.0000 - 1.0000
    risk_level      VARCHAR(10)     NOT NULL,  -- LOW, MEDIUM, HIGH, CRITICAL
    computed_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    features_json   JSONB,          -- Feature values used for prediction
    model_version   VARCHAR(20),
    CONSTRAINT uk_attrition_employee_date UNIQUE (employee_id, computed_at)
);

CREATE INDEX idx_attrition_employee ON ai_attrition_scores(employee_id);
CREATE INDEX idx_attrition_risk ON ai_attrition_scores(risk_level);
CREATE INDEX idx_attrition_computed ON ai_attrition_scores(computed_at);

-- ─── Notifications ───
CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    recipient_id    BIGINT          REFERENCES users(id),
    type            VARCHAR(50)     NOT NULL,   -- EMAIL, SMS, IN_APP
    channel         VARCHAR(20)     NOT NULL,
    subject         VARCHAR(200),
    body            TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',  -- PENDING, SENT, FAILED, DELIVERED
    error_message   TEXT,
    metadata_json   JSONB,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_type ON notifications(type);

-- ─── Announcements ───
CREATE TABLE announcements (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    author_id       BIGINT          REFERENCES users(id),
    department_id   BIGINT          REFERENCES departments(id),  -- NULL = all departments
    priority        VARCHAR(10)     DEFAULT 'NORMAL',  -- LOW, NORMAL, HIGH, URGENT
    published       BOOLEAN         NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
