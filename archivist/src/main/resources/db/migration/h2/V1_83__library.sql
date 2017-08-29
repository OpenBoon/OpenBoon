
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, 'Library', 1, 1450709321000, 1, 1450709321000, '{}', 0);

/*
 * Permissions for the date folders.  Managers can add folders, users can read.
 */
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='manager' AND str_type='group'),
  (SELECT pk_folder FROM folder WHERE str_name='Library' AND pk_parent=0), 3);

INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='everyone' AND str_type='group'),
  (SELECT pk_folder FROM folder WHERE str_name='Library' AND pk_parent=0), 1);


UPDATE folder SET pk_parent=0 WHERE str_name != 'Users' AND pk_parent=0;

