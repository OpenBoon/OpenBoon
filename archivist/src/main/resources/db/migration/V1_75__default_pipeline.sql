
ALTER TABLE pipeline ADD COLUMN bool_standard BOOLEAN NOT NULL DEFAULT 0;
UPDATE pipeline SET bool_standard=1 WHERE pk_pipeline=(SELECT pk_pipeline FROM pipeline WHERE str_name LIKE '%Vision');









