
CREATE INDEX task_error_pk_task_idx ON task_error(pk_task);
CREATE INDEX task_error_pk_job_idx ON task_error(pk_job);
CREATE INDEX task_error_pk_asset_idx ON task_error(pk_asset);
CREATE INDEX task_error_str_path_idx ON task_error(str_path);
CREATE INDEX task_error_str_processor_idx ON task_error(str_processor);
CREATE INDEX task_error_time_created_idx ON task_error(time_created);
