ALTER TABLE projects ADD COLUMN interview_document TEXT;
ALTER TABLE projects ADD COLUMN interview_revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE projects ADD COLUMN reviewed_revision BIGINT NOT NULL DEFAULT -1;
ALTER TABLE projects ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE questions ADD COLUMN focus_capability VARCHAR(80);
ALTER TABLE questions ADD COLUMN question_kind VARCHAR(32) NOT NULL DEFAULT 'INTERVIEW';
ALTER TABLE export_artifacts ADD COLUMN source_revision BIGINT;
ALTER TABLE users ADD COLUMN study_condition VARCHAR(32);
CREATE TABLE interview_revisions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    revision BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    document_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (project_id, revision)
);
CREATE INDEX idx_interview_revisions_project ON interview_revisions(project_id);
