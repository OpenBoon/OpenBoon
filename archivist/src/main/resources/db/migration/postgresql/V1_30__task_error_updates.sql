
ALTER TABLE task_error ADD COLUMN fti_keywords TSVECTOR;
UPDATE task_error SET fti_keywords=to_tsvector(
    REPLACE(str_path, '/', ' ') || ' ' || str_path || ' ' || str_processor || ' ' || str_message);
ALTER TABLE task_error ALTER COLUMN fti_keywords SET NOT NULL;
CREATE INDEX task_error_fti_keywords_idx ON task_error USING gin(fti_keywords);
