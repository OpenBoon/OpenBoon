
ALTER TABLE processor ADD COLUMN int_type INTEGER NOT NULL DEFAULT 0;

UPDATE processor SET int_type=0 WHERE str_type='document';
UPDATE processor SET int_type=4 WHERE str_type='generate';

ALTER TABLE processor DROP COLUMN str_type;
