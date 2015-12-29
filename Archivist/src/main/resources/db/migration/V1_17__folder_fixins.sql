
UPDATE folder SET str_name='Folders' WHERE str_name='public';
UPDATE folder SET str_name='Users' WHERE str_name='users';
UPDATE folder SET str_name='Exports' WHERE str_name='exports';

/*
 * Fixed permissions for the /Folders folder.  Users get read/write.
 */
DELETE FROM folder_acl WHERE pk_folder = (SELECT pk_folder FROM folder WHERE str_name='Folders');
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::user'),
  (SELECT pk_folder FROM folder WHERE str_name='Folders'), 3);


/*
 * Added the ingest folder.
 */
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, 'Ingests', 1, 1450709321000, 1, 1450709321000, '{"filters": {"existFields": ["ingest.id"]}}', 0);

/*
 * Permissions for the ingests folder.  Managers can add folders, users can read.
 */
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::manager'),
  (SELECT pk_folder FROM folder WHERE str_name='Ingests'), 3);

INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::user'),
  (SELECT pk_folder FROM folder WHERE str_name='Ingests'), 1);


/*
 * Create Admin's user folder
 */
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (
  (SELECT pk_folder FROM folder WHERE str_name='Users' AND pk_parent=0),
  'admin', 1, 1450709321000, 1, 1450709321000, null, 1);

INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='user::admin'),
  (SELECT pk_folder FROM folder WHERE str_name='admin'), 3);
