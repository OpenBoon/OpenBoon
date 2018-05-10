
CREATE TABLE organization (
  pk_organization UUID PRIMARY KEY,
  str_name TEXT NOT NULL
);

INSERT INTO organization(pk_organization, str_name) values
  ('00000000-9998-8888-7777-666666666666', 'Zorroa');

ALTER TABLE permission ADD COLUMN pk_organization UUID;
UPDATE permission SET pk_organization='00000000-9998-8888-7777-666666666666';
ALTER TABLE permission ALTER COLUMN pk_organization SET NOT NULL;

ALTER TABLE folder ADD COLUMN pk_organization UUID;
UPDATE folder SET pk_organization='00000000-9998-8888-7777-666666666666';
ALTER TABLE folder ALTER COLUMN pk_organization SET NOT NULL;

ALTER TABLE users ADD COLUMN pk_organization UUID;
UPDATE users SET pk_organization='00000000-9998-8888-7777-666666666666';
ALTER TABLE users ALTER COLUMN pk_organization SET NOT NULL;

ALTER TABLE shared_link ADD COLUMN pk_organization UUID;
UPDATE shared_link SET pk_organization='00000000-9998-8888-7777-666666666666';
ALTER TABLE shared_link ALTER COLUMN pk_organization SET NOT NULL;
