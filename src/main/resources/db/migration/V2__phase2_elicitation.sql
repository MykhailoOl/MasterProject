ALTER TABLE projects
    ADD COLUMN llm_provider VARCHAR(32),
    ADD COLUMN simplify_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_llm_credentials (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider        VARCHAR(32)  NOT NULL,
    api_key_enc     TEXT         NOT NULL,
    last_verified_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_llm_credentials_user_id ON user_llm_credentials (user_id);

CREATE TABLE project_categories (
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    category         VARCHAR(64)  NOT NULL,
    mandatory        BOOLEAN      NOT NULL DEFAULT FALSE,
    max_questions    INT          NOT NULL,
    questions_asked  INT          NOT NULL DEFAULT 0,
    UNIQUE (project_id, category)
);

CREATE INDEX idx_project_categories_project_id ON project_categories (project_id);
