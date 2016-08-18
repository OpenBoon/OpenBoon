
CREATE TABLE plugin (
  pk_plugin INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(32) NOT NULL,
  str_lang VARCHAR(16) NOT NULL,
  str_description VARCHAR(128) NOT NULL,
  str_version VARCHAR(32) NOT NULL,
  str_publisher VARCHAR(64) NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL
);

CREATE UNIQUE INDEX pluginstr_name_idx ON plugin (str_name);

CREATE TABLE processor (
  pk_processor INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_plugin INTEGER NOT NULL REFERENCES plugin (pk_plugin) ON DELETE CASCADE,
  str_name VARCHAR(255) NOT NULL,
  str_short_name VARCHAR(255) NOT NULL,
  str_module VARCHAR(255) NOT NULL,
  str_type VARCHAR(24) NOT NULL,
  str_description VARCHAR(255) NOT NULL,
  json_display TEXT NOT NULL DEFAULT '{}',
  json_ext TEXT NOT NULL DEFAULT '[]',
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL
);

CREATE INDEX processor_pk_plugin_idx ON processor (pk_plugin);
CREATE UNIQUE INDEX processor_str_name_idx ON processor (str_name);
