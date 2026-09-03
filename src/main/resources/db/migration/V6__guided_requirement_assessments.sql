ALTER TABLE requirement_slots
    ADD COLUMN assessment_json TEXT;

ALTER TABLE questions
    ADD COLUMN focus_criterion VARCHAR(64);
