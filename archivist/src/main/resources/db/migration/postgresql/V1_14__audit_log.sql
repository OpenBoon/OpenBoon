
CREATE TABLE asset (
  pk_asset UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  pk_user_created UUID NOT NULL REFERENCES users(pk_user),
  pk_user_modified UUID NOT NULL REFERENCES users(pk_user),
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  json_document JSONB NOT NULL,
  int_update_count BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX asset_pk_organization_idx on asset (pk_asset);
CREATE INDEX asset_pk_user_created_idx on asset (pk_user_created);
CREATE INDEX asset_pk_user_modified_idx on asset (pk_user_modified);

