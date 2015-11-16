CREATE TABLE export (
  pk_export INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_user_created VARCHAR(128) NOT NULL,
  time_created BIGINT NOT NULL,
  int_state INTEGER NOT NULL DEFAULT 0,
  str_note TEXT NOT NULL,
  json_search TEXT NOT NULL,
  json_options TEXT NOT NULL
);

CREATE TABLE export_output (
  pk_export_output INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_export INTEGER NOT NULL REFERENCES export(pk_export),
  str_user_created VARCHAR(128) NOT NULL,
  time_created BIGINT NOT NULL,
  json_factory TEXT NOT NULL

);

CREATE INDEX export_output_pk_export_idx ON export_output (pk_export);
