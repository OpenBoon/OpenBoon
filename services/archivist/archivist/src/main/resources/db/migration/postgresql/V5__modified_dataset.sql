--
-- Name: project bool_modified; Type: COLUMN; Schema: zorroa; Owner: zorroa
--

ALTER TABLE data_set ADD bool_modified bool NOT NULL DEFAULT false;
COMMENT ON COLUMN data_set.bool_modified IS 'Indicates if a Data set needs retrain';
