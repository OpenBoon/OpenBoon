ALTER TABLE api_key ADD system_key bool NOT NULL DEFAULT false;
COMMENT ON COLUMN api_key.system_key IS 'True if is a System Key';

UPDATE api_key SET system_key = true WHERE name = 'job-runner'