
CREATE TABLE jblob (
  pk_jblob INTEGER PRIMARY KEY AUTO_INCREMENT,
  int_version BIGINT NOT NULL DEFAULT 1,
  str_app VARCHAR(255) NOT NULL,
  str_feature VARCHAR(255) NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  json_data VARCHAR(524288) NOT NULL,
  user_created INTEGER NOT NULL,
  time_created BIGINT NOT NULL,
  user_modified INTEGER NOT NULL,
  time_modified BIGINT NOT NULL
);

CREATE UNIQUE INDEX jblob_uniq_idx ON jblob (str_app, str_feature, str_name);

CREATE TABLE jblob_acl (
  pk_permission INTEGER NOT NULL REFERENCES permission(pk_permission) ON DELETE CASCADE,
  pk_jblob INTEGER NOT NULL REFERENCES jblob(pk_jblob) ON DELETE CASCADE,
  int_access INTEGER,
  PRIMARY KEY (pk_jblob, pk_permission)
);

CREATE INDEX jblob_acl_pk_permission_idx ON jblob_acl (pk_permission);



