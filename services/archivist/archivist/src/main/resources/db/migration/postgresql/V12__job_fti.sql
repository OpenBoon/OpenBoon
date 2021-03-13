ALTER TABLE job ADD fti_keywords tsvector NOT NULL DEFAULT ''::tsvector;

UPDATE job SET fti_keywords = to_tsvector(str_name);