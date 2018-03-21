

CREATE OR REPLACE FUNCTION bitand(INTEGER, INTEGER) RETURNS INTEGER AS $$
BEGIN
  RETURN $1 & $2;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION current_time_millis() returns BIGINT as $$
BEGIN
RETURN (extract(epoch from clock_timestamp()) * 1000)::BIGINT;
end;
$$ LANGUAGE plpgsql;

---
---

CREATE TABLE pipeline (
  pk_pipeline UUID PRIMARY KEY,
  int_type SMALLINT NOT NULL,
  str_name TEXT NOT NULL,
  str_description TEXT NOT NULL,
  bool_standard BOOLEAN DEFAULT 'f' NOT NULL,
  json_processors TEXT DEFAULT '[]' NOT NULL,
  int_version INTEGER DEFAULT 1 NOT NULL
);

CREATE UNIQUE INDEX pipeline_name_uidx ON pipeline(str_name);

---
---

CREATE TABLE plugin(
  pk_plugin UUID PRIMARY KEY,
  str_name TEXT NOT NULL,
  str_lang TEXT NOT NULL,
  str_description TEXT NOT NULL,
  str_version TEXT NOT NULL,
  str_publisher TEXT NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  str_md5 TEXT
);

CREATE UNIQUE INDEX plugin_str_name_idx ON plugin(str_name);

---
---

CREATE TABLE folder(
  pk_folder UUID PRIMARY KEY,
  pk_parent UUID,
  pk_dyhi UUID,
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  str_name TEXT NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  bool_recursive BOOLEAN DEFAULT 'f' NOT NULL,
  bool_dyhi_root BOOLEAN DEFAULT 'f' NOT NULL,
  str_dyhi_field VARCHAR(128),
  int_child_count INTEGER DEFAULT 0 NOT NULL,
  bool_tax_root BOOLEAN DEFAULT 'f' NOT NULL,
  json_attrs TEXT DEFAULT '{}' NOT NULL,
  json_search TEXT
);

CREATE UNIQUE INDEX folder_unique_siblings_idx ON folder(pk_parent, str_name);
CREATE INDEX folder_pk_dyhi_idx ON folder(pk_dyhi);

---
---

CREATE TABLE command(
  pk_command UUID PRIMARY KEY,
  pk_user_created UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_started BIGINT DEFAULT -1 NOT NULL,
  time_stopped BIGINT DEFAULT -1 NOT NULL,
  int_state SMALLINT DEFAULT 0 NOT NULL,
  int_type SMALLINT NOT NULL,
  str_message TEXT,
  int_total_count BIGINT DEFAULT 0 NOT NULL,
  int_success_count BIGINT DEFAULT 0 NOT NULL,
  int_error_count BIGINT DEFAULT 0 NOT NULL,
  json_args TEXT DEFAULT '[]' NOT NULL
);

CREATE INDEX command_pk_user_idx ON command(pk_user_created);
CREATE INDEX command_int_state_idx ON command(int_state);

---
---

CREATE TABLE pipeline_acl(
  pk_permission UUID NOT NULL,
  pk_pipeline UUID NOT NULL,
  int_access INTEGER DEFAULT 0 NOT NULL
);

ALTER TABLE pipeline_acl ADD CONSTRAINT pipeline_acl_pkey PRIMARY KEY(pk_pipeline, pk_permission);
CREATE INDEX pipeline_acl_permission_idx ON pipeline_acl(pk_permission);

---
---

CREATE TABLE taxonomy(
  pk_taxonomy UUID,
  pk_folder UUID NOT NULL,
  bool_active BOOLEAN DEFAULT 'f' NOT NULL,
  time_started BIGINT DEFAULT 0 NOT NULL,
  time_stopped BIGINT DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX taxonomy_dyhi_pk_folder ON taxonomy(pk_folder);

---
---

CREATE TABLE settings(
  str_name TEXT PRIMARY KEY,
  str_value TEXT
);

---
---

CREATE TABLE dyhi(
  pk_dyhi UUID PRIMARY KEY,
  pk_folder UUID NOT NULL,
  pk_user_created UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_executed BIGINT DEFAULT -1 NOT NULL,
  bool_enabled BOOLEAN DEFAULT 't' NOT NULL,
  bool_working BOOLEAN DEFAULT 'f' NOT NULL,
  int_levels INTEGER NOT NULL,
  json_levels TEXT DEFAULT '{}' NOT NULL
);

CREATE UNIQUE INDEX dyhi_pk_folder ON dyhi(pk_folder);

---
---

CREATE TABLE job(
  pk_job UUID PRIMARY KEY,
  pk_user_created UUID NOT NULL,
  str_name TEXT NOT NULL,
  int_type SMALLINT DEFAULT 0 NOT NULL,
  int_state SMALLINT DEFAULT 0 NOT NULL,
  time_started BIGINT NOT NULL,
  str_root_path TEXT NOT NULL,
  json_args TEXT DEFAULT '{}' NOT NULL,
  json_env TEXT DEFAULT '{}' NOT NULL
);


CREATE INDEX job_name_idx ON job(str_name);
CREATE INDEX job_type_idx ON job(int_type);
CREATE INDEX job_state_idx ON job(int_state);

---
---

CREATE TABLE export_file (
  pk_export_file UUID PRIMARY KEY,
  pk_job UUID NOT NULL,
  str_name TEXT NOT NULL,
  str_path TEXT NOT NULL,
  str_mime_type TEXT NOT NULL,
  int_size BIGINT NOT NULL DEFAULT 0,
  time_created BIGINT NOT NULL
);

CREATE UNIQUE INDEX export_file_pk_job_str_name_uidx ON export_file(pk_job, str_name);

---
---

CREATE TABLE users(
  pk_user UUID PRIMARY KEY,
  pk_permission UUID NOT NULL,
  pk_folder UUID NOT NULL,
  str_username TEXT NOT NULL,
  str_password TEXT NOT NULL,
  str_email TEXT NOT NULL,
  str_firstname TEXT,
  str_lastname TEXT,
  bool_enabled BOOLEAN NOT NULL,
  hmac_key TEXT,
  str_source TEXT DEFAULT 'local' NOT NULL,
  bool_reset_pass BOOLEAN DEFAULT 'f' NOT NULL,
  str_reset_pass_token TEXT,
  time_reset_pass BIGINT DEFAULT 0 NOT NULL,
  time_last_login BIGINT DEFAULT 0 NOT NULL,
  int_login_count INT DEFAULT 0 NOT NULL,
  json_settings TEXT DEFAULT '{}' NOT NULL
);

CREATE UNIQUE INDEX user_str_username_idx ON users(str_username);
CREATE UNIQUE INDEX user_str_email_idx ON users(str_email);

---
---

CREATE TABLE user_permission(
  pk_permission UUID NOT NULL,
  pk_user UUID NOT NULL,
  str_source TEXT NOT NULL DEFAULT 'local',
  bool_immutable BOOLEAN DEFAULT 'f' NOT NULL
);

ALTER TABLE user_permission ADD CONSTRAINT user_permission_pkey PRIMARY KEY(pk_user, pk_permission);
CREATE INDEX user_permission_pk_permission_idx ON user_permission(pk_permission);

---
---
CREATE TABLE preset(
  pk_preset UUID PRIMARY KEY,
  str_name TEXT NOT NULL,
  json_settings TEXT NOT NULL
);

---
---

CREATE TABLE job_count(
  pk_job UUID PRIMARY KEY,
  int_task_total_count INTEGER DEFAULT 0 NOT NULL,
  int_task_completed_count INTEGER DEFAULT 0 NOT NULL,
  int_task_state_waiting_count INTEGER DEFAULT 0 NOT NULL,
  int_task_state_queued_count INTEGER DEFAULT 0 NOT NULL,
  int_task_state_running_count INTEGER DEFAULT 0 NOT NULL,
  int_task_state_success_count INTEGER DEFAULT 0 NOT NULL,
  int_task_state_failure_count INTEGER DEFAULT 0 NOT NULL,
  int_task_state_skipped_count INTEGER DEFAULT 0 NOT NULL,
  time_updated BIGINT DEFAULT current_time_millis() NOT NULL
);

---
---

CREATE TABLE migration(
  pk_migration UUID PRIMARY KEY,
  int_type INTEGER NOT NULL,
  str_name TEXT NOT NULL,
  str_path TEXT NOT NULL,
  int_version INTEGER DEFAULT 1 NOT NULL,
  time_applied BIGINT DEFAULT -1 NOT NULL,
  bool_enabled BOOLEAN DEFAULT 't' NOT NULL,
  int_patch INTEGER DEFAULT 0 NOT NULL
);

INSERT INTO migration(pk_migration, int_type, str_name, str_path, int_version, time_applied, bool_enabled, int_patch) values
  ('54745A12-38C2-466C-990A-10C3FA936640', 0, 'archivist', 'classpath:/db/mappings/archivist/*.json', 10, 1454449960000, TRUE, 0),
  ('54745A12-38C2-466C-990A-10C3FA936641', 0, 'analyst', 'classpath:/db/mappings/analyst/*.json', 1, 1454449960000, TRUE, 0),
  ('54745A12-38C2-466C-990A-10C3FA936643',0, 'job_logs', 'classpath:/db/mappings/job_logs/*.json', 1, 1454449960000, TRUE, 0),
  ('54745A12-38C2-466C-990A-10C3FA936644',0, 'user_logs', 'classpath:/db/mappings/user_logs/*.json', 1, 1454449960000, TRUE, 0);

---
---

CREATE TABLE folder_acl(
  pk_permission UUID NOT NULL,
  pk_folder UUID NOT NULL,
  int_access INTEGER DEFAULT 0 NOT NULL
);

ALTER TABLE folder_acl ADD CONSTRAINT folder_acl_pkey PRIMARY KEY(pk_folder, pk_permission);
CREATE INDEX folder_acl_pk_permission_idx ON folder_acl(pk_permission);

---
---

CREATE TABLE preset_permission(
  pk_permission UUID NOT NULL,
  pk_preset UUID NOT NULL
);
ALTER TABLE preset_permission ADD CONSTRAINT preset_permission_pkey PRIMARY KEY(pk_permission, pk_preset);
CREATE INDEX preset_permission_pk_preset ON preset_permission(pk_preset);

---
---

CREATE TABLE job_stat(
  pk_job UUID PRIMARY KEY,
  int_asset_total_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_create_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_error_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_warning_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_update_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_replace_count INTEGER DEFAULT 0 NOT NULL
);
---
---

CREATE TABLE task_stat(
  pk_task UUID PRIMARY KEY,
  pk_job UUID NOT NULL,
  int_asset_total_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_create_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_error_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_warning_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_update_count INTEGER DEFAULT 0 NOT NULL,
  int_asset_replace_count INTEGER DEFAULT 0 NOT NULL
);

CREATE INDEX task_stat_pk_job_idx ON task_stat(pk_job);

---
---

CREATE TABLE processor(
  pk_processor UUID PRIMARY KEY,
  pk_plugin UUID NOT NULL,
  str_name TEXT NOT NULL,
  str_short_name TEXT NOT NULL,
  str_module TEXT NOT NULL,
  str_description TEXT NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  json_display TEXT DEFAULT '{}' NOT NULL,
  json_filters TEXT DEFAULT '[]' NOT NULL,
  json_file_types TEXT DEFAULT '[]' NOT NULL,
  int_type INTEGER DEFAULT 0 NOT NULL
);

CREATE INDEX processor_pk_plugin_idx ON processor(pk_plugin);
CREATE UNIQUE INDEX processor_str_name_idx ON processor(str_name);

---
---

CREATE TABLE folder_trash(
  pk_folder_trash UUID,
  pk_folder UUID NOT NULL,
  pk_parent UUID NOT NULL,
  pk_user_deleted UUID NOT NULL,
  pk_user_created UUID NOT NULL,
  str_opid TEXT NOT NULL,
  str_name TEXT NOT NULL,
  bool_recursive BOOLEAN NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  time_deleted BIGINT NOT NULL,
  bool_primary BOOLEAN NOT NULL,
  int_order INTEGER NOT NULL,
  json_attrs TEXT DEFAULT '{}' NOT NULL,
  json_acl TEXT,
  json_search TEXT
);

CREATE INDEX folder_trash_pk_folder ON folder_trash(pk_folder);
CREATE INDEX folder_trash_pk_parent ON folder_trash(pk_parent);
CREATE INDEX folder_trash_str_opid ON folder_trash(str_opid);

---
---

CREATE TABLE shared_link(
  pk_shared_link UUID,
  pk_user_created UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_expired BIGINT NOT NULL,
  json_state TEXT,
  json_users TEXT
);

CREATE INDEX shared_link_pk_user_idx ON shared_link(pk_user_created);

---
---

CREATE TABLE permission(
  pk_permission UUID PRIMARY KEY,
  str_name TEXT NOT NULL,
  str_description TEXT NOT NULL,
  str_type TEXT NOT NULL,
  bool_immutable BOOLEAN DEFAULT 'f' NOT NULL,
  bool_user_assignable BOOLEAN DEFAULT 't' NOT NULL,
  bool_obj_assignable BOOLEAN DEFAULT 't' NOT NULL,
  str_authority TEXT NOT NULL,
  str_source TEXT NOT NULL DEFAULT 'local'
);

CREATE UNIQUE INDEX permission_name_and_type_uniq_idx ON permission(str_name, str_type);

---
---

CREATE TABLE task(
  pk_task UUID PRIMARY KEY,
  pk_parent UUID,
  pk_job UUID NOT NULL,
  pk_depend_parent UUID,
  int_state SMALLINT DEFAULT 0 NOT NULL,
  time_started BIGINT DEFAULT -1 NOT NULL,
  time_stopped BIGINT DEFAULT -1 NOT NULL,
  time_created BIGINT NOT NULL,
  time_state_change BIGINT NOT NULL,
  time_ping BIGINT DEFAULT 0 NOT NULL,
  int_order SMALLINT DEFAULT 0 NOT NULL,
  int_exit_status SMALLINT DEFAULT -1 NOT NULL,
  str_host TEXT,
  str_name TEXT NOT NULL,
  int_depend_count INTEGER DEFAULT 0 NOT NULL
);

CREATE INDEX task_parent_idx ON task(pk_parent);
CREATE INDEX task_pk_job_idx ON task(pk_job);
CREATE INDEX task_state_idx ON task(int_state);
CREATE INDEX task_order_idx ON task(int_order);

---
---

CREATE TABLE field_hide (
  pk_field TEXT PRIMARY KEY,
  bool_manual BOOLEAN DEFAULT 'f' NOT NULL
);

---
---

CREATE TABLE jblob (
  pk_jblob UUID PRIMARY KEY,
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  int_version BIGINT NOT NULL DEFAULT 1,
  str_app TEXT NOT NULL,
  str_feature TEXT NOT NULL,
  str_name TEXT NOT NULL,
  json_data TEXT NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL
);

CREATE UNIQUE INDEX jblob_uidx ON jblob (str_app, str_feature, str_name);

----
----
----

CREATE TABLE jblob_acl (
  pk_permission UUID NOT NULL,
  pk_jblob UUID NOT NULL,
  int_access INTEGER,
  PRIMARY KEY (pk_jblob, pk_permission)
);

CREATE INDEX jblob_acl_pk_permission_idx ON jblob_acl (pk_permission);

----
----
----

CREATE TABLE request (
  pk_request UUID PRIMARY KEY,
  pk_folder UUID,
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  int_state SMALLINT NOT NULL DEFAULT 0,
  int_type SMALLINT NOT NULL,
  str_comment TEXT NOT NULL,
  json_cc TEXT NOT NULL
);

CREATE INDEX request_int_state_idx ON request(int_state);

----
----
----

INSERT INTO permission(pk_permission, str_name, str_description, str_type, bool_immutable, bool_user_assignable, bool_obj_assignable, str_authority) values
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA0', 'admin', 'The admin user', 'user', TRUE, FALSE, TRUE, 'user::admin'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA1', 'administrator', 'Superuser, can do and access everything.', 'zorroa', TRUE, TRUE, TRUE, 'zorroa::administrator'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA2', 'manager', 'Manage users and permissions.', 'zorroa', TRUE, TRUE, TRUE, 'zorroa::manager'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA3', 'developer', 'Manage and create pipelines.', 'zorroa', TRUE, TRUE, TRUE, 'zorroa::developer'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA4', 'share', 'Modify all permissions.', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::share'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA5', 'everyone', 'A standard user of the system.', 'zorroa', TRUE, TRUE, TRUE, 'zorroa::everyone'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA6', 'export', 'User can export all data.', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::export'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA7', 'read', 'User can read all data.', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::read'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA8', 'write', 'User can write all data.', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::write'),
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA9', 'librarian', 'Can manage the /library folder', 'zorroa', TRUE, TRUE, FALSE, 'zorroa::librarian');


INSERT INTO folder(pk_folder, pk_parent, str_name, pk_user_created, time_created, pk_user_modified, time_modified, bool_recursive, pk_dyhi, bool_dyhi_root, str_dyhi_field, int_child_count, bool_tax_root, json_attrs, json_search) values
  ('00000000-0000-0000-0000-000000000000', NULL, '/', '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, FALSE, NULL, FALSE, NULL, 2, FALSE, '{}', '{}'),
  ('00000000-2395-4E71-9E4C-DACCEEF6AD51', '00000000-0000-0000-0000-000000000000', 'Users', '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, FALSE, NULL, FALSE, NULL, 1, FALSE, '{}', NULL),
  ('00000000-2395-4E71-9E4C-DACCEEF6AD52', '00000000-0000-0000-0000-000000000000', 'Library', '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, FALSE, NULL, FALSE, NULL, 0, FALSE, '{}', '{}'),
  ('00000000-2395-4E71-9E4C-DACCEEF6AD53', '00000000-2395-4E71-9E4C-DACCEEF6AD51', 'admin', '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, '00000000-7B0B-480E-8C36-F06F04AED2F1', 1450709321000, TRUE, NULL, FALSE, NULL, 0, FALSE, '{}', NULL);

INSERT INTO folder_acl(pk_permission, pk_folder, int_access) VALUES
  --- admin::user, write on /users/admin
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA0', '00000000-2395-4E71-9E4C-DACCEEF6AD53', 3),
  --- zorroa::everyone, read /
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA5', '00000000-0000-0000-0000-000000000000', 1),
  --- zorroa::everyone, read /users
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA5', '00000000-2395-4E71-9E4C-DACCEEF6AD51', 1),
  --- zorroa::everyone, read /library
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA5', '00000000-2395-4E71-9E4C-DACCEEF6AD52', 1),
  --- zorroa::librarian, read-write /library
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA9', '00000000-2395-4E71-9E4C-DACCEEF6AD52', 3);


---
---

INSERT INTO users(pk_user, str_username, str_password, str_email, str_firstname, str_lastname, bool_enabled, str_source, pk_permission, pk_folder, bool_reset_pass, str_reset_pass_token, time_reset_pass, json_settings) values
  ('00000000-7B0B-480E-8C36-F06F04AED2F1', 'admin', '$2a$10$26Ekb4MDeUdz75G4V2u6geSuI1Hn4jrHUvZafK5M2iHdz5s9oLGyK', 'admin@com', 'Joe', 'Admin', TRUE, 'local', '00000000-FC08-4E4A-AA7A-A183F42C9FA0', '00000000-2395-4E71-9E4C-DACCEEF6AD53', TRUE, NULL, 0, '{}');


INSERT INTO user_permission(pk_permission, pk_user, bool_immutable) VALUES
  --- admin's specific user permission - immutable
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA0', '00000000-7B0B-480E-8C36-F06F04AED2F1', TRUE),
  --- admin's administrator group
  ('00000000-FC08-4E4A-AA7A-A183F42C9FA1', '00000000-7B0B-480E-8C36-F06F04AED2F1', FALSE);

---
---
---


ALTER TABLE user_permission ADD CONSTRAINT constraint_fe86 foreign key(pk_permission) REFERENCES permission(pk_permission) ON DELETE CASCADE ;
ALTER TABLE preset_permission ADD CONSTRAINT constraint_f76 foreign key(pk_preset) REFERENCES preset(pk_preset) ON DELETE CASCADE ;
ALTER TABLE preset_permission ADD CONSTRAINT constraint_f7 foreign key(pk_permission) REFERENCES permission(pk_permission) ON DELETE CASCADE ;
ALTER TABLE users ADD CONSTRAINT constraint_27e foreign key(pk_permission) REFERENCES permission(pk_permission) ;
ALTER TABLE users ADD CONSTRAINT constraint_27e3 foreign key(pk_folder) REFERENCES folder(pk_folder) ;
ALTER TABLE folder_acl ADD CONSTRAINT constraint_eb foreign key(pk_folder) REFERENCES folder(pk_folder) ON DELETE CASCADE ;
ALTER TABLE folder_acl ADD CONSTRAINT constraint_e foreign key(pk_permission) REFERENCES permission(pk_permission) ON DELETE CASCADE ;
ALTER TABLE folder ADD CONSTRAINT constraint_7bf foreign key(pk_parent) REFERENCES folder(pk_folder) ;
ALTER TABLE processor ADD CONSTRAINT constraint_64d foreign key(pk_plugin) REFERENCES plugin(pk_plugin) ON DELETE CASCADE ;
ALTER TABLE task ADD CONSTRAINT constraint_272d foreign key(pk_job) REFERENCES job(pk_job) ON DELETE CASCADE ;
ALTER TABLE dyhi ADD CONSTRAINT constraint_204 foreign key(pk_folder) REFERENCES folder(pk_folder) ON DELETE CASCADE ;
ALTER TABLE taxonomy ADD CONSTRAINT constraint_1f0 foreign key(pk_folder) REFERENCES folder(pk_folder) ;
ALTER TABLE task ADD CONSTRAINT constraint_272 foreign key(pk_parent) REFERENCES task(pk_task) ON delete set NULL ;


CREATE OR REPLACE FUNCTION trigger_update_folder_child_count() RETURNS TRIGGER AS $$
BEGIN
  if old.pk_parent > new.pk_parent then
    update folder set int_child_count=int_child_count -1 where pk_folder=old.pk_parent;
    update folder set int_child_count=int_child_count +1 where pk_folder=new.pk_parent;
  else
    update folder set int_child_count=int_child_count +1 where pk_folder=new.pk_parent;
    update folder set int_child_count=int_child_count -1 where pk_folder=old.pk_parent;
  end if;
  return NEW;
end
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_after_folder_update AFTER UPDATE ON folder
  FOR EACH ROW WHEN (old.pk_parent != new.pk_parent)
  EXECUTE PROCEDURE trigger_update_folder_child_count();


CREATE OR REPLACE FUNCTION trigger_increment_folder_child_count() RETURNS TRIGGER AS $$
BEGIN
  UPDATE folder SET int_child_count=int_child_count +1 WHERE pk_folder=NEW.pk_parent;
return NEW;
end
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_after_folder_insert after insert ON folder
FOR EACH ROW EXECUTE PROCEDURE trigger_increment_folder_child_count();


CREATE OR REPLACE FUNCTION trigger_decrement_folder_child_count() RETURNS TRIGGER AS $$
BEGIN
  UPDATE folder SET int_child_count=int_child_count -1 WHERE pk_folder=OLD.pk_parent;
  RETURN OLD;
END
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_after_folder_delete AFTER DELETE ON folder
FOR EACH ROW EXECUTE PROCEDURE trigger_decrement_folder_child_count();

CREATE OR REPLACE FUNCTION trigger_update_job_state() RETURNS TRIGGER AS $$
BEGIN
  IF NEW.int_task_state_success_count + NEW.int_task_state_failure_count + NEW.int_task_state_skipped_count = NEW.int_task_total_count THEN
    UPDATE job set int_state=2 where pk_job=NEW.pk_job AND int_state=0;
  ELSE
    UPDATE job set int_state=0 where pk_job=NEW.pk_job AND int_state=2;
  END IF;

  RETURN NEW;
END
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_update_job_state AFTER UPDATE ON job_count
FOR EACH ROW EXECUTE PROCEDURE trigger_update_job_state();

