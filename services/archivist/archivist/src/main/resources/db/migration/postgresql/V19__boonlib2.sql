ALTER TABLE boonlib DROP COLUMN license;
ALTER TABLE boonlib DROP COLUMN entity_type;
ALTER TABLE boonlib ADD COLUMN  entity_type TEXT NOT NULL DEFAULT 'Classification';
