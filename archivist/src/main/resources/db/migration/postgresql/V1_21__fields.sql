
DROP TABLE field_hide;

---
--- Provides per-organization custom field naming counters which
--- ensure that custom fields getField a unique name.
---
CREATE TABLE field_alloc (
  pk_field_alloc UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  int_attr_type SMALLINT NOT NULL,
  int_count SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX field_alloc_uniq_idx ON field_alloc(pk_organization, int_attr_type);

---
--- The Field table describes what fields are exposed to the user.
---
CREATE TABLE field (
  pk_field UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  str_name TEXT NOT NULL,
  str_attr_name TEXT NOT NULL,
  int_attr_type SMALLINT NOT NULL,
  bool_editable BOOLEAN NOT NULL,
  bool_custom BOOLEAN NOT NULL,
  bool_keywords BOOLEAN NOT NULL DEFAULT 'f',
  float_keywords_boost FLOAT NOT NULL DEFAULT 1.0
);

CREATE UNIQUE INDEX field_uniq_idx ON field(pk_organization, str_attr_name);
CREATE INDEX field_str_attr_name_idx ON field(str_attr_name);
CREATE INDEX field_int_attr_type_idx ON field(int_attr_type);

---
--- Stores manual edits to fields.
---
CREATE TABLE field_edit (
  pk_field_edit UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  pk_field UUID NOT NULL REFERENCES field(pk_field) ON DELETE CASCADE,
  pk_asset UUID NOT NULL,
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  json_old_value JSONB,
  json_new_value JSONB
);

CREATE INDEX field_edit_pk_asset_idx ON field_edit (pk_asset);
CREATE INDEX field_edit_pk_organization_idx ON field_edit (pk_organization);
CREATE UNIQUE INDEX field_edit_uniq_idx ON field_edit (pk_field, pk_asset, pk_organization);


---
--- A set off fields for visual display
---
CREATE TABLE field_set (
  pk_field_set UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  str_name TEXT NOT NULL,
  str_link_expr TEXT
);

---
--- Contains members of a field se.
---
CREATE TABLE field_set_member (
  pk_field_set_member UUID PRIMARY KEY,
  pk_field UUID NOT NULL REFERENCES field(pk_field) ON DELETE CASCADE,
  pk_field_set UUID NOT NULL REFERENCES field_set(pk_field_set) ON DELETE CASCADE,
  int_order SMALLINT DEFAULT 0
);

CREATE UNIQUE INDEX field_set_member_uniq_idx ON field_set_member(pk_field, pk_field_set);
CREATE INDEX field_set_member_pk_field_set_idx ON field_set_member(pk_field_set);
