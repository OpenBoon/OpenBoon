
--- This permission is something everyone gets
INSERT INTO permission (str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable)
  VALUES ('everyone', 'A standard user of the system.  All users get this permission.', 'group', true, true, true);

--- Set up permissions on the root folder so the manager can reader/write
--- but everyone else can only read.

--- Manager can read/write root
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_type='group' AND str_name='manager'),
  (SELECT pk_folder FROM folder WHERE pk_folder=0), 3);

--- Everyone can read root
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_type='group' AND str_name='everyone'),
  (SELECT pk_folder FROM folder WHERE pk_folder=0), 1);

--- Everyone can read Users
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_type='group' AND str_name='everyone'),
  (SELECT pk_folder FROM folder WHERE pk_parent=0 AND str_name='Users'), 1);

-- Use a insert driven by select to add everyone to all existing users.
INSERT INTO user_permission (pk_user, pk_permission) SELECT pk_user,
  (SELECT pk_permission FROM permission WHERE str_type='group' AND str_name='everyone') FROM user;

--- Admin user gets administrator
INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (
  (SELECT pk_permission FROM permission WHERE str_name='administrator' AND str_type='group'),
  (SELECT pk_user FROM user WHERE str_username='admin'), 0
);

-- The user table now gets links to the users permission and home folder.
ALTER TABLE user ADD pk_permission INTEGER;
ALTER TABLE user ADD pk_folder INTEGER;

UPDATE user SET pk_permission=(SELECT pk_permission FROM
  permission WHERE str_name=user.str_username AND str_type='user');

UPDATE user SET pk_folder=(SELECT pk_folder FROM
  folder WHERE str_name=user.str_username LIMIT 1);


ALTER TABLE user ALTER COLUMN pk_permission SET NOT NULL;
ALTER TABLE user ADD FOREIGN KEY (pk_permission) REFERENCES permission (pk_permission);
ALTER TABLE user ALTER COLUMN pk_folder SET NOT NULL;
ALTER TABLE user ADD FOREIGN KEY (pk_folder) REFERENCES folder (pk_folder);
