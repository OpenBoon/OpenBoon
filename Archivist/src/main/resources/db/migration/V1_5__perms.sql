
--- Need to keep track of available roles

CREATE TABLE permission (
  pk_permission INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(128) NOT NULL,
  str_description VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX permission_str_name_idx ON permission (str_name);

INSERT INTO permission (str_name, str_description) VALUES ('systems', 'Can adjust server configuration');
INSERT INTO permission (str_name, str_description) VALUES ('manager', 'Can modify user accounts');
INSERT INTO permission (str_name, str_description) VALUES ('search', 'Can search the archive');
INSERT INTO permission (str_name, str_description) VALUES ('ingest', 'Can ingest data');
INSERT INTO permission (str_name, str_description) VALUES ('export', 'Can export data');

CREATE TABLE map_permission_to_user (
  pk_permission INTEGER NOT NULL REFERENCES permission (pk_permission) ON DELETE CASCADE,
  pk_user INTEGER NOT NULL REFERENCES user (pk_user) ON DELETE CASCADE,
  PRIMARY KEY (pk_user, pk_permission)
);

CREATE INDEX map_permission_to_user_pk_permission ON map_permission_to_user (pk_permission);

/**
 * Give the admin user all the permissions.
 */
INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='systems'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='manager'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='search'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='ingest'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='export'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

ALTER TABLE user DROP COLUMN list_roles;
