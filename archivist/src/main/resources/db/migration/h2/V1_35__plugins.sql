CREATE TABLE plugin (
  pk_plugin INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  str_description TEXT NOT NULL,
  str_version VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX plugin_name_version_uniq_idx ON plugin (str_name, str_version);

CREATE TABLE processor (
  pk_processor INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_plugin INT NOT NULL REFERENCES plugin(pk_plugin),
  int_type INT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  json_display TEXT
);

CREATE UNIQUE INDEX processor_name_idx ON processor (str_name);

ALTER TABLE analyst DROP COLUMN json_ingestor_classes;
