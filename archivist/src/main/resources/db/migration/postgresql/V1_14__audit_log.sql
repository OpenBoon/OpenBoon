
CREATE TABLE asset (
  pk_asset UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  pk_user_created UUID NOT NULL REFERENCES users(pk_user),
  pk_user_modified UUID NOT NULL REFERENCES users(pk_user),
  int_state SMALLINT NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  json_document JSONB NOT NULL,
  int_update_count BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX asset_pk_organization_idx on asset (pk_organization);
CREATE INDEX asset_pk_user_created_idx on asset (pk_user_created);
CREATE INDEX asset_pk_user_modified_idx on asset (pk_user_modified);
CREATE INDEX asset_pk_time_modified_idx on asset (time_modified);
CREATE INDEX asset_pk_int_state_idx on asset (int_state);

---

CREATE TABLE auditlog (
  pk_auditlog UUID PRIMARY KEY,
  --- cannot have FK link due to CDV
  pk_asset UUID NOT NULL,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  pk_user_created UUID NOT NULL REFERENCES users(pk_user),
  time_created BIGINT NOT NULL,
  int_type SMALLINT NOT NULL,
  str_field TEXT,
  json_value TEXT,
  str_message TEXT NOT NULL
);

CREATE INDEX auditlog_pk_organization_idx on auditlog (pk_organization);
CREATE INDEX auditlog_pk_asset_idx on auditlog (pk_asset);
CREATE INDEX auditlog_pk_user_created_idx on auditlog (pk_user_created);
CREATE INDEX auditlog_time_created_idx on auditlog (time_created);
CREATE INDEX auditlog_int_type_idx on auditlog (int_type);
CREATE INDEX auditlog_str_field_idx on auditlog (str_field);
