
--- Drop these constraints for internal users
ALTER TABLE users ALTER COLUMN pk_permission DROP NOT NULL;
ALTER TABLE users ALTER COLUMN pk_folder DROP NOT NULL;

--- Create a batch user for the default org
INSERT INTO users(pk_user, pk_organization, str_username, str_password, str_email, str_firstname, str_lastname, bool_enabled, str_source, pk_permission, pk_folder, bool_reset_pass, str_reset_pass_token, time_reset_pass, json_settings, hmac_key) values
  ('00000000-7B0B-480E-8C36-F06F04AED2B2', '00000000-9998-8888-7777-666666666666', 'batch_user_00000000-9998-8888-7777-666666666666', '$2a$10$61erDtvsR5jl3smXZQvkVOf5ei9P.wZtyCkyLUsC.BGFyTbg6AMf2', 'batch_user_00000000-9998-8888-7777-666666666666@zorroa.com', 'Batch', 'User', TRUE, 'internal', null,  null, TRUE, NULL, 0, '{}', '2d8f3bf77bb200687b0e6f4e026cf5a22c1003ea06a7f96164af92eca75447b0');

--- The batch user can write
INSERT INTO user_permission(pk_permission, pk_user, bool_immutable) VALUES
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA8', '00000000-7B0B-480E-8C36-F06F04AED2B2', TRUE),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA5', '00000000-7B0B-480E-8C36-F06F04AED2B2', TRUE);


--- A table for storing queued files coming from pubsub.
CREATE TABLE queued_file (
  pk_queued_file UUID PRIMARY KEY,
  pk_organization UUID REFERENCES organization (pk_organization),
  pk_pipeline UUID REFERENCES pipeline (pk_pipeline),
  asset_id UUID NOT NULL,
  json_metadata TEXT NOT NULL,
  str_path TEXT NOT NULL,
  time_created BIGINT NOT NULL
);

CREATE INDEX queued_file_pk_organization_idx ON queued_file (pk_organization);
CREATE INDEX queued_file_pk_pipeline_idx ON queued_file (pk_pipeline);

