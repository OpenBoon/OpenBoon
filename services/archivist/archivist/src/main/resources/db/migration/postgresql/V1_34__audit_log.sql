ALTER TABLE auditlog RENAME COLUMN json_value TO json_new_value;

ALTER TABLE auditlog ADD COLUMN json_old_value TEXT;

CREATE INDEX auditlog_json_new_value_idx on auditlog (json_new_value);
CREATE INDEX auditlog_json_old_value_idx on auditlog (json_old_value);
