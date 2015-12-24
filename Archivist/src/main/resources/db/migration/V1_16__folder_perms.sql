
DELETE FROM permission;
ALTER TABLE permission ADD str_type VARCHAR(32) NOT NULL;

/*
 * Internal permissions  are hard referenced in @RestController classes and are flagged
 * so they cannot be deleted.  User permissions are also managed by the archivist
 * and cannot be manually deleted via the API.
 */
ALTER TABLE permission ADD bool_immutable BOOLEAN NOT NULL DEFAULT 0;
UPDATE permission SET bool_immutable=1;

DROP TABLE map_permission_to_user;
CREATE TABLE user_permission (
  pk_permission INTEGER NOT NULL REFERENCES permission (pk_permission) ON DELETE CASCADE,
  pk_user INTEGER NOT NULL REFERENCES user (pk_user) ON DELETE CASCADE,
  bool_immutable BOOLEAN NOT NULL DEFAULT 0,
  PRIMARY KEY (pk_user, pk_permission)
);

CREATE INDEX user_permission_pk_permission_idx ON user_permission (pk_permission);

/**
 * Create the standard permissions and one for the admin user.
 */
INSERT INTO permission (str_name, str_type, str_description, bool_immutable) VALUES
  ('group::superuser', 'group', 'Superuser, can do and access everything', 1),
  ('group::manager', 'group', 'Can manage configuration such as public folders, permissions, ingests', 1),
  ('group::user', 'group', 'Can search the archive, create rooms, notes, and their own folders', 1),
  ('group::export', 'group', 'Can export data', 1),
  ('user::admin', 'user', 'The Admin user', 1);

/**
 * Give the admin all permissions except super user, otherwise permissions are hard to test.
 */
INSERT INTO user_permission (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='user::admin'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO user_permission (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='group::manager'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO user_permission (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='group::user'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

INSERT INTO user_permission (pk_permission, pk_user) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='group::export'),
  (SELECT pk_user FROM user WHERE str_username='admin')
);

CREATE TABLE folder_acl (
  pk_permission INTEGER NOT NULL REFERENCES permission (pk_permission) ON DELETE CASCADE,
  pk_folder INTEGER NOT NULL REFERENCES folder (pk_folder) ON DELETE CASCADE,
  int_access INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (pk_folder, pk_permission)
);
CREATE INDEX folder_acl_pk_permission_idx ON folder_acl (pk_permission);

/**
 * A /public folder for storing the public hierarchy.
 */
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, 'public', 1, 1450709321000, 1, 1450709321000, null, 0);

/*
 * Permissions for the public folder.
 */
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::manager'),
  (SELECT pk_folder FROM folder WHERE str_name='public'), 3);

INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::user'),
  (SELECT pk_folder FROM folder WHERE str_name='public'), 1);


/**
 * The export folder is for storing folders created by the export system.
 */
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, 'exports', 1, 1450709321000, 1, 1450709321000, '{"filters": {"existFields": ["exports"]}}', 0);

/*
 * Permissions for the exports folder
 */
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::export'),
  (SELECT pk_folder FROM folder WHERE str_name='exports'), 3);

INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::user'),
  (SELECT pk_folder FROM folder WHERE str_name='exports'), 1);
