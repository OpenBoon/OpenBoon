
ALTER TABLE cluster_lock ADD COLUMN int_combine_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cluster_lock ADD COLUMN bool_allow_combine BOOLEAN NOT NULL DEFAULT 'f';

ALTER TABLE dyhi RENAME COLUMN bool_working to bool_canceled;