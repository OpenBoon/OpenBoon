DROP TABLE filter;
DROP TABLE action;
DROP TABLE matcher;

CREATE TABLE filter (
  pk_filter INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_description VARCHAR(128) NOT NULL,
  bool_enabled BOOLEAN NOT NULL DEFAULT 1,
  json_search TEXT NOT NULL,
  json_acl TEXT NOT NULL
);


