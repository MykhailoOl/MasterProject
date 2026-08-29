ALTER TABLE users
    ADD COLUMN display_name VARCHAR(120);

ALTER TABLE questions
    ADD COLUMN options_json TEXT;
