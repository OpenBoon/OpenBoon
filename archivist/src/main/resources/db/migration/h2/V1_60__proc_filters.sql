
ALTER TABLE processor DROP COLUMN json_ext;
ALTER TABLE processor ADD COLUMN json_filters TEXT NOT NULL DEFAULT '[]';


