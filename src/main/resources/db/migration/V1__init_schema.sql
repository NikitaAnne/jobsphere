-- ============================================================
-- V1__init_schema.sql — JobSphere initial database schema
-- ============================================================

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id                BIGSERIAL PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    phone             VARCHAR(30),
    location          VARCHAR(255),
    bio               TEXT,
    skills            TEXT,
    profile_image_url VARCHAR(500),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255)
);

-- User ↔ Role mapping (many-to-many)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Jobs
CREATE TABLE IF NOT EXISTS jobs (
    id                    BIGSERIAL PRIMARY KEY,
    title                 VARCHAR(255)   NOT NULL,
    description           TEXT           NOT NULL,
    company               VARCHAR(255)   NOT NULL,
    location              VARCHAR(255)   NOT NULL,
    is_remote             BOOLEAN        NOT NULL DEFAULT FALSE,
    job_type              VARCHAR(30)    NOT NULL,
    experience_level      VARCHAR(30),
    required_skills       TEXT,
    salary_min            NUMERIC(12, 2),
    salary_max            NUMERIC(12, 2),
    salary_currency       VARCHAR(10)    DEFAULT 'USD',
    application_deadline  DATE,
    status                VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    posted_by             BIGINT         NOT NULL REFERENCES users (id),
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255)
);

CREATE INDEX idx_jobs_status    ON jobs (status);
CREATE INDEX idx_jobs_posted_by ON jobs (posted_by);
CREATE INDEX idx_jobs_location  ON jobs (location);

-- Applications
CREATE TABLE IF NOT EXISTS applications (
    id               BIGSERIAL PRIMARY KEY,
    candidate_id     BIGINT       NOT NULL REFERENCES users (id),
    job_id           BIGINT       NOT NULL REFERENCES jobs (id),
    status           VARCHAR(30)  NOT NULL DEFAULT 'APPLIED',
    cover_letter     TEXT,
    resume_url       VARCHAR(500),
    recruiter_notes  TEXT,
    ai_match_score   INT,
    ai_match_details TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    CONSTRAINT uk_application_candidate_job UNIQUE (candidate_id, job_id)
);

CREATE INDEX idx_applications_job       ON applications (job_id);
CREATE INDEX idx_applications_candidate ON applications (candidate_id);
CREATE INDEX idx_applications_status    ON applications (status);

-- ============================================================
-- Seed initial roles
-- ============================================================
INSERT INTO roles (name) VALUES ('ROLE_ADMIN')     ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_RECRUITER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_CANDIDATE') ON CONFLICT (name) DO NOTHING;
