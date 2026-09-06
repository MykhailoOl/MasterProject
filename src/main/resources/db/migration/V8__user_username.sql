ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(24);

UPDATE users
SET username = LOWER('user' || id::text)
WHERE username IS NULL OR BTRIM(username) = '';

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username_lower
    ON users ((LOWER(username)));

ALTER TABLE users
    DROP COLUMN IF EXISTS display_name;
