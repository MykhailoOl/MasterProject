ALTER TABLE completeness_snapshots
    ADD COLUMN session_id BIGINT REFERENCES elicitation_sessions (id) ON DELETE CASCADE,
    ADD COLUMN answer_id BIGINT REFERENCES answers (id) ON DELETE CASCADE,
    ADD COLUMN answered_category VARCHAR(64),
    ADD COLUMN sequence_number INT;

CREATE INDEX idx_completeness_snapshots_session_id
    ON completeness_snapshots (session_id);

CREATE UNIQUE INDEX idx_completeness_snapshots_answer_id
    ON completeness_snapshots (answer_id);

CREATE UNIQUE INDEX idx_completeness_snapshots_session_sequence
    ON completeness_snapshots (session_id, sequence_number);
