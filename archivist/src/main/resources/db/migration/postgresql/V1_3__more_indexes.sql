CREATE INDEX job_time_started_idx ON job(time_started);
CREATE INDEX job_user_created_idx ON job(pk_user_created);

DROP INDEX export_file_pk_job_str_name_uidx;
CREATE INDEX export_file_pk_job_idx ON export_file(pk_job);

CREATE INDEX user_pk_permission_idx ON users(pk_permission);
CREATE INDEX user_pk_folder_idx ON users(pk_folder);

CREATE INDEX shared_link_time_expired_idx ON shared_link(time_expired);

CREATE INDEX permission_str_authority_idx ON permission(str_authority);

CREATE INDEX request_pk_folder_idx ON request(pk_folder);
