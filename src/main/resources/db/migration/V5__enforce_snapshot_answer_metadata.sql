DELETE FROM completeness_snapshots
WHERE session_id IS NULL
   OR answer_id IS NULL
   OR answered_category IS NULL
   OR sequence_number IS NULL;

ALTER TABLE completeness_snapshots
    ALTER COLUMN session_id SET NOT NULL,
    ALTER COLUMN answer_id SET NOT NULL,
    ALTER COLUMN answered_category SET NOT NULL,
    ALTER COLUMN sequence_number SET NOT NULL;
