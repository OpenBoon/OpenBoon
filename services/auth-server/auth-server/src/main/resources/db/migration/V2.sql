ALTER TABLE api_key ADD enabled bool NOT NULL DEFAULT true;
COMMENT ON COLUMN api_key.enabled IS 'True if a Key is Enabled';