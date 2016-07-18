
CREATE TABLE dyhi (
  pk_dyhi INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_folder INTEGER NOT NULL REFERENCES folder (pk_folder) ON DELETE CASCADE,
  int_user_created INTEGER NOT NULL,
  time_created BIGINT NOT NULL,
  time_executed BIGINT NOT NULL DEFAULT -1,
  bool_enabled BOOLEAN NOT NULL DEFAULT 1,
  bool_working BOOLEAN NOT NULL DEFAULT 0,
  int_levels INTEGER NOT NULL,
  json_levels TEXT NOT NULL
);

--- A folder can only have 1 dyhi currently.
CREATE UNIQUE INDEX dyhi_pk_folder ON dyhi (pk_folder);

--- Need to keep track which folders are part of dhyi.
ALTER TABLE folder ADD pk_dyhi INTEGER;
CREATE INDEX folder_pk_dyhi_idx ON folder (pk_dyhi);

ALTER TABLE folder ADD bool_dyhi_root BOOLEAN NOT NULL DEFAULT 0;

-- Never used
ALTER TABLE folder DROP COLUMN json_permissions;
