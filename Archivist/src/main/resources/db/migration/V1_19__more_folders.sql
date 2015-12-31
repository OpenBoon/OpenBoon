
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, 'Date', 1, 1450709321000, 1, 1450709321000, '{}', 0);

/*
 * Permissions for the date folders.  Managers can add folders, users can read.
 */
INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::manager'),
  (SELECT pk_folder FROM folder WHERE str_name='Date'), 3);

INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (
  (SELECT pk_permission FROM permission  WHERE str_name='group::user'),
  (SELECT pk_folder FROM folder WHERE str_name='Date'), 1);


INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, '★ Rating', 1, 1450709321000, 1, 1450709321000, '{"filter":{"existFields":["Xmp.Rating"]},"querySet":false}', 0);

INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES ((SELECT pk_folder FROM folder WHERE pk_parent=0 AND str_name='★ Rating'), '★', 1, 1450709321000, 1, 1450709321000,
  '{"filter":{"fieldTerms":[{"field":"Xmp.Rating","terms":["1"]}]},"confidence":0.0,"fuzzy":true,"querySet":false}', 0);
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES ((SELECT pk_folder  FROM folder WHERE pk_parent=0 AND str_name='★ Rating'), '★★', 1, 1450709321000, 1, 1450709321000,
  '{"filter":{"fieldTerms":[{"field":"Xmp.Rating","terms":["2"]}]},"confidence":0.0,"fuzzy":true,"querySet":false}', 0);
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES ((SELECT pk_folder  FROM folder WHERE pk_parent=0 AND str_name='★ Rating'), '★★★', 1, 1450709321000, 1, 1450709321000,
  '{"filter":{"fieldTerms":[{"field":"Xmp.Rating","terms":["3"]}]},"confidence":0.0,"fuzzy":true,"querySet":false}', 0);
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES ((SELECT pk_folder  FROM folder WHERE pk_parent=0 AND str_name='★ Rating'), '★★★★', 1, 1450709321000, 1, 1450709321000,
  '{"filter":{"fieldTerms":[{"field":"Xmp.Rating","terms":["4"]}]},"confidence":0.0,"fuzzy":true,"querySet":false}', 0);
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES ((SELECT pk_folder  FROM folder WHERE pk_parent=0 AND str_name='★ Rating'), '★★★★★', 1, 1450709321000, 1, 1450709321000,
  '{"filter":{"fieldTerms":[{"field":"Xmp.Rating","terms":["5"]}]},"confidence":0.0,"fuzzy":true,"querySet":false}', 0);

