
CREATE TABLE export_file (
  pk_export_file BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_job BIGINT NOT NULL REFERENCES job (pk_job),
  str_name VARCHAR(128) NOT NULL,
  str_path VARCHAR(255) NOT NULL,
  str_mime_type VARCHAR(32) NOT NULL,
  int_size BIGINT NOT NULL DEFAULT 0,
  time_created BIGINT NOT NULL
);

CREATE UNIQUE INDEX export_file_pk_job_str_name_uidx ON export_file (pk_job, str_name);

