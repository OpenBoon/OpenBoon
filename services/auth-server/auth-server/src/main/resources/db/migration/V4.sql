ALTER TABLE api_key ADD hidden bool NOT NULL DEFAULT false;
UPDATE api_key SET hidden = true WHERE name = 'job-runner';