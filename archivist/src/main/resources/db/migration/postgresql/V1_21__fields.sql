
DROP TABLE field_hide;

---
--- Provides per-organization custom field naming counters which
--- ensure that custom fields get a unique name.
---
CREATE TABLE field_alloc (
  pk_field_alloc UUID PRIMARY KEY,
  pk_organization UUID NOT NULL REFERENCES organization(pk_organization),
  int_attr_type SMALLINT NOT NULL,
  int_count SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX field_alloc_uniq_idx ON field_alloc(pk_organization, int_attr_type);

---
--- The Field table stores field metadata.  For a field to be visible
--- it must be in this table.
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
  bool_custom BOOLEAN NOT NULL
);

CREATE UNIQUE INDEX field_uniq_idx ON field(pk_organization, str_attr_name);



