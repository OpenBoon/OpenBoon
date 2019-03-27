
ALTER TABLE auditlog RENAME COLUMN str_field TO str_attr_name;
ALTER INDEX auditlog_str_field_idx RENAME TO auditlog_str_attr_name_idx;

ALTER TABLE auditlog ADD COLUMN pk_field UUID REFERENCES field(pk_field);
CREATE INDEX auditlog_pk_field_idx ON auditlog (pk_field);
