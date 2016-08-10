ALTER TABLE permission ADD bool_user_assignable BOOLEAN NOT NULL DEFAULT 1;
ALTER TABLE permission ADD bool_obj_assignable BOOLEAN NOT NULL DEFAULT 1;

UPDATE permission SET bool_user_assignable=0,bool_obj_assignable=0 WHERE str_type='internal';
UPDATE permission SET bool_user_assignable=0,bool_obj_assignable=1 WHERE str_type='user';

CREATE TABLE filter (
  pk_filter INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_description VARCHAR(128) NOT NULL,
  bool_enabled BOOLEAN NOT NULL DEFAULT 1,
  bool_match_all BOOLEAN NOT NULL,
  --- An elastic query composed of all matchers.
  json_query TEXT,
  --- The ID of the percolate query.
  int_percolate INTEGER
);

CREATE TABLE action (
  pk_action INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_filter INTEGER NOT NULL REFERENCES filter (pk_filter) ON DELETE CASCADE,
  int_type SMALLINT NOT NULL,
  pk_permission INTEGER REFERENCES permission (pk_permission) ON DELETE CASCADE,
  pk_folder INTEGER REFERENCES folder (pk_folder) ON DELETE CASCADE
);

CREATE TABLE matcher (
  pk_matcher INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_filter INTEGER NOT NULL REFERENCES filter (pk_filter) ON DELETE CASCADE,
  str_attr VARCHAR(128) NOT NULL,
  str_cmp VARCHAR(16) NOT NULL,
  str_value VARCHAR(128)
);
