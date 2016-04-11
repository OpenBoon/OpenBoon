
INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, bool_recursive)
  VALUES ((SELECT pk_folder FROM folder WHERE pk_parent=(SELECT pk_folder FROM folder WHERE str_name='Users' AND pk_parent=0)
   AND str_name='admin'), 'Trash', 1, 1450709321000, 1, 1450709321000, 0);
