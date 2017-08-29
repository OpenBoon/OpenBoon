ALTER TABLE user ADD COLUMN bool_reset_pass BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE user ADD COLUMN str_reset_pass_token VARCHAR(64);
ALTER TABLE user ADD COLUMN time_reset_pass BIGINT NOT NULL DEFAULT 0;

UPDATE user SET bool_reset_pass=1 WHERE str_username='admin';



