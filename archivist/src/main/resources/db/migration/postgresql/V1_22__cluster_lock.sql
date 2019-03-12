
ALTER TABLE cluster_lock ADD COLUMN int_combine_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cluster_lock ADD COLUMN bool_allow_combine BOOLEAN NOT NULL DEFAULT 'f';

ALTER TABLE dyhi DROP COLUMN bool_working;

ALTER TABLE taxonomy DROP COLUMN bool_active;
ALTER TABLE taxonomy DROP COLUMN time_started;
ALTER TABLE taxonomy DROP COLUMN time_stopped;

ALTER TABLE taxonomy ADD COLUMN pk_user_created UUID;
UPDATE taxonomy SET pk_user_created=(SELECT folder.pk_user_created FROM folder where taxonomy.pk_folder = folder.pk_folder);
ALTER TABLE taxonomy ALTER COLUMN pk_user_created SET NOT NULL;

ALTER TABLE taxonomy ADD COLUMN time_created BIGINT;
UPDATE taxonomy SET time_created=1551899363123;
ALTER TABLE taxonomy ALTER COLUMN time_created SET NOT NULL;
