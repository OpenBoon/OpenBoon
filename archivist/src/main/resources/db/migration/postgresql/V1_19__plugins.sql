
DROP TABLE processor;

CREATE TABLE processor(
  pk_processor UUID PRIMARY KEY,
  str_name TEXT NOT NULL,
  str_file TEXT NOT NULL,
  str_type TEXT NOT NULL,
  str_description TEXT DEFAULT '' NOT NULL,
  time_updated BIGINT NOT NULL,
  json_display JSONB DEFAULT '[]'::jsonb NOT NULL,
  list_file_types TEXT[] DEFAULT '{}' NOT NULL,
  fti_keywords TSVECTOR NOT NULL
);

CREATE UNIQUE INDEX processor_str_name_idx ON processor(str_name);
CREATE INDEX processor_fti_keywords_idx ON processor USING gin(fti_keywords);

