CREATE OR REPLACE FUNCTION random_string(int)
    RETURNS text
AS
$$
  SELECT array_to_string(
    ARRAY(
       SELECT substring(
         '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
         FROM (random() * 26)::int FOR 1)
       FROM generate_series(1, $1)), '')
$$ LANGUAGE sql;

ALTER TABLE api_key ADD access_key TEXT;
UPDATE api_key SET access_key = random_string(24);

ALTER TABLE api_key ALTER COLUMN access_key SET NOT NULL;
ALTER TABLE api_key RENAME COLUMN shared_key TO secret_key;

CREATE UNIQUE INDEX api_key_access_key_uniq_idx ON api_key (access_key);