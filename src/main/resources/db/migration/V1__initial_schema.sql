-- Initial schema for the thesis elicitation application

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    title           VARCHAR(255) NOT NULL,
    initial_idea    TEXT         NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_user_id ON projects (user_id);

CREATE TABLE elicitation_sessions (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    condition_tag   VARCHAR(32)  NOT NULL DEFAULT 'GUIDED',
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_elicitation_sessions_project_id ON elicitation_sessions (project_id);

CREATE TABLE questions (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES elicitation_sessions (id) ON DELETE CASCADE,
    category        VARCHAR(64)  NOT NULL,
    question_text   TEXT         NOT NULL,
    simplified_text TEXT,
    question_order  INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_session_id ON questions (session_id);

CREATE TABLE answers (
    id              BIGSERIAL PRIMARY KEY,
    question_id     BIGINT       NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    answer_text     TEXT         NOT NULL,
    answered_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_answers_question_id ON answers (question_id);

CREATE TABLE requirement_slots (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    category        VARCHAR(64)  NOT NULL,
    value           TEXT,
    completeness    DOUBLE PRECISION NOT NULL DEFAULT 0,
    source          VARCHAR(32)  NOT NULL DEFAULT 'USER',
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, category)
);

CREATE INDEX idx_requirement_slots_project_id ON requirement_slots (project_id);

CREATE TABLE completeness_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    scores_json     TEXT         NOT NULL,
    total_score     DOUBLE PRECISION NOT NULL,
    captured_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_completeness_snapshots_project_id ON completeness_snapshots (project_id);

CREATE TABLE export_artifacts (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    export_type     VARCHAR(32)  NOT NULL,
    content         TEXT         NOT NULL,
    generated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_export_artifacts_project_id ON export_artifacts (project_id);
