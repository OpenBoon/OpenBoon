
CREATE TABLE folder_trash (
  pk_folder_trash INTEGER PRIMARY KEY AUTO_INCREMENT,
  pk_folder INTEGER NOT NULL,
  pk_parent INTEGER NOT NULL,
  str_opid VARCHAR(36) NOT NULL,
  str_name VARCHAR(128) NOT NULL,
  json_acl TEXT,
  json_search TEXT,
  bool_recursive BOOLEAN NOT NULL,
  user_created INTEGER NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  user_deleted INTEGER NOT NULL,
  time_deleted BIGINT NOT NULL,
  bool_primary BOOLEAN NOT NULL,
  int_order INTEGER NOT NULL
);

CREATE INDEX folder_trash_pk_folder ON folder_trash (pk_folder);
CREATE INDEX folder_trash_pk_parent ON folder_trash (pk_parent);
CREATE INDEX folder_trash_str_opid ON folder_trash (str_opid);
