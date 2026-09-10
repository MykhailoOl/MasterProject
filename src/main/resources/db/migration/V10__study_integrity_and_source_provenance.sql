ALTER TABLE projects ADD COLUMN collection_protocol VARCHAR(64) NOT NULL DEFAULT 'LEGACY_UNVERIFIED';
ALTER TABLE projects ALTER COLUMN collection_protocol DROP DEFAULT;
ALTER TABLE answers ADD COLUMN provenance VARCHAR(32) NOT NULL DEFAULT 'LEGACY_UNKNOWN';
ALTER TABLE answers ALTER COLUMN provenance DROP DEFAULT;
ALTER TABLE questions ADD COLUMN generation_origin VARCHAR(32) NOT NULL DEFAULT 'LEGACY_UNKNOWN';
ALTER TABLE questions ALTER COLUMN generation_origin DROP DEFAULT;
ALTER TABLE questions ADD COLUMN generation_reason VARCHAR(100);
ALTER TABLE questions ADD COLUMN llm_call_id VARCHAR(255);
ALTER TABLE questions ADD COLUMN prompt_version VARCHAR(100);
ALTER TABLE users ADD COLUMN assignment_method VARCHAR(32) NOT NULL DEFAULT 'UNASSIGNED';
UPDATE users SET assignment_method = 'MANUAL' WHERE study_condition IS NOT NULL;
ALTER TABLE elicitation_sessions ADD COLUMN assignment_method VARCHAR(32) NOT NULL DEFAULT 'LEGACY_UNKNOWN';
ALTER TABLE elicitation_sessions ADD COLUMN question_budget INTEGER NOT NULL DEFAULT 24;
ALTER TABLE elicitation_sessions ADD COLUMN end_reason VARCHAR(32);
ALTER TABLE elicitation_sessions ADD COLUMN study_model VARCHAR(255);
ALTER TABLE elicitation_sessions ADD COLUMN study_enrolled BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE llm_call_audits (
    id VARCHAR(255) PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phase VARCHAR(255) NOT NULL,
    prompt_version VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    outcome VARCHAR(255) NOT NULL,
    requested_temperature DOUBLE PRECISION NOT NULL,
    duration_ms BIGINT NOT NULL,
    metadata_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_llm_call_project ON llm_call_audits(project_id, created_at);
