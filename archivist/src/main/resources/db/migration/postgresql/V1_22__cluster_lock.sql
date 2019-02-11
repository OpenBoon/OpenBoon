
ALTER TABLE cluster_lock ADD COLUMN int_rerun INTEGER NOT NULL DEFAULT 0;

ALTER TABLE dyhi RENAME COLUMN bool_working to bool_canceled;