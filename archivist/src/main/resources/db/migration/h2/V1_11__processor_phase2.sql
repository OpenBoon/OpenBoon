ALTER TABLE pipeline DROP COLUMN list_processors;
ALTER TABLE pipeline ADD json_processors TEXT NOT NULL;
