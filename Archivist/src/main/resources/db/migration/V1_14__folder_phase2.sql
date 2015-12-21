CREATE TABLE folder (
  pk_folder INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_parent INTEGER REFERENCES folder(pk_folder),
  str_name VARCHAR(255) NOT NULL,
  user_created INTEGER NOT NULL,
  time_created BIGINT NOT NULL,
  user_modified INTEGER NOT NULL,
  time_modified BIGINT NOT NULL,
  json_search TEXT,
  json_permissions TEXT,
  bool_recursive BOOLEAN NOT NULL DEFAULT 'f'
);

CREATE UNIQUE INDEX folder_unique_siblings_idx ON folder (pk_parent, str_name);

INSERT INTO folder (pk_folder, pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, NULL, '/', 1, 1450709321000, 1, 1450709321000, '{}', 0);

INSERT INTO folder (pk_parent, str_name, user_created, time_created, user_modified, time_modified, json_search, bool_recursive)
  VALUES (0, 'users', 1, 1450709321000, 1, 1450709321000, '{}', 0);


