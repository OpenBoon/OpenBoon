

CREATE OR REPLACE FUNCTION ZORROA.BITAND(integer, integer) RETURNS INTEGER AS $$
BEGIN
  RETURN $1 & $2;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ZORROA.CURRENT_TIMESTAMP() RETURNS BIGINT AS $$
BEGIN
RETURN (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::bigint;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE ZORROA.HIDE_FIELD (
  PK_FIELD TEXT PRIMARY KEY
);

---
---

CREATE TABLE ZORROA.PIPELINE(
  PK_PIPELINE SERIAL PRIMARY KEY,
  INT_TYPE SMALLINT NOT NULL,
  STR_NAME VARCHAR(32) NOT NULL,
  STR_DESCRIPTION VARCHAR(255) NOT NULL,
  BOOL_STANDARD BOOLEAN DEFAULT 'f' NOT NULL,
  JSON_PROCESSORS TEXT DEFAULT '[]' NOT NULL,
  INT_VERSION INTEGER DEFAULT 1 NOT NULL
);

CREATE UNIQUE INDEX PIPELINE_NAME_UNIQ_IDX ON ZORROA.PIPELINE(STR_NAME);

---
---

CREATE TABLE ZORROA.PLUGIN(
  PK_PLUGIN SERIAL PRIMARY KEY,
  STR_NAME VARCHAR(32) NOT NULL,
  STR_LANG VARCHAR(16) NOT NULL,
  STR_DESCRIPTION VARCHAR(255) NOT NULL,
  STR_VERSION VARCHAR(32) NOT NULL,
  STR_PUBLISHER VARCHAR(64) NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_MODIFIED BIGINT NOT NULL,
  STR_MD5 VARCHAR(48)
);

CREATE UNIQUE INDEX PLUGIN_STR_NAME_IDX ON ZORROA.PLUGIN(STR_NAME);

---
---

CREATE TABLE ZORROA.FOLDER(
  PK_FOLDER BIGSERIAL PRIMARY KEY,
  PK_PARENT INTEGER,
  STR_NAME VARCHAR(255) NOT NULL,
  USER_CREATED INTEGER NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  USER_MODIFIED INTEGER NOT NULL,
  TIME_MODIFIED BIGINT NOT NULL,
  BOOL_RECURSIVE BOOLEAN DEFAULT 'f' NOT NULL,
  PK_DYHI INTEGER,
  BOOL_DYHI_ROOT BOOLEAN DEFAULT 'f' NOT NULL,
  STR_DYHI_FIELD VARCHAR(128),
  INT_CHILD_COUNT INTEGER DEFAULT 0 NOT NULL,
  BOOL_TAX_ROOT BOOLEAN DEFAULT 'f' NOT NULL,
  JSON_ATTRS TEXT DEFAULT '{}' NOT NULL,
  JSON_SEARCH TEXT
);

CREATE UNIQUE INDEX FOLDER_UNIQUE_SIBLINGS_IDX ON ZORROA.FOLDER(PK_PARENT, STR_NAME);
CREATE INDEX FOLDER_PK_DYHI_IDX ON ZORROA.FOLDER(PK_DYHI);

---
---

CREATE TABLE ZORROA.COMMAND(
  PK_COMMAND SERIAL PRIMARY KEY,
  PK_USER INT NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_STARTED BIGINT DEFAULT -1 NOT NULL,
  TIME_STOPPED BIGINT DEFAULT -1 NOT NULL,
  INT_STATE SMALLINT DEFAULT 0 NOT NULL,
  INT_TYPE SMALLINT NOT NULL,
  STR_MESSAGE VARCHAR(255),
  INT_TOTAL_COUNT BIGINT DEFAULT 0 NOT NULL,
  INT_SUCCESS_COUNT BIGINT DEFAULT 0 NOT NULL,
  INT_ERROR_COUNT BIGINT DEFAULT 0 NOT NULL,
  JSON_ARGS TEXT DEFAULT '[]' NOT NULL
);

CREATE INDEX COMMAND_PK_USER_IDX ON ZORROA.COMMAND(PK_USER);
CREATE INDEX COMMAND_INT_STATE_IDX ON ZORROA.COMMAND(INT_STATE);

---
---

CREATE TABLE ZORROA.PIPELINE_ACL(
  PK_PERMISSION INTEGER NOT NULL,
  PK_PIPELINE INTEGER NOT NULL,
  INT_ACCESS INTEGER DEFAULT 0 NOT NULL
);

ALTER TABLE ZORROA.PIPELINE_ACL ADD CONSTRAINT CONSTRAINT_9F PRIMARY KEY(PK_PIPELINE, PK_PERMISSION);
CREATE INDEX PIPELINE_ACL_PERMISSION_IDX ON ZORROA.PIPELINE_ACL(PK_PERMISSION);

---
---

CREATE TABLE ZORROA.TAXONOMY(
  PK_TAXONOMY SERIAL PRIMARY KEY,
  PK_FOLDER INTEGER NOT NULL,
  BOOL_ACTIVE BOOLEAN DEFAULT 'f' NOT NULL,
  TIME_STARTED BIGINT DEFAULT 0 NOT NULL,
  TIME_STOPPED BIGINT DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX TAXONOMY_DYHI_PK_FOLDER ON ZORROA.TAXONOMY(PK_FOLDER);

---
---

CREATE TABLE ZORROA.SETTINGS(
  STR_NAME VARCHAR(128) PRIMARY KEY,
  STR_VALUE TEXT
);

---
---

CREATE TABLE ZORROA.DYHI(
  PK_DYHI SERIAL PRIMARY KEY,
  PK_FOLDER INTEGER NOT NULL,
  INT_USER_CREATED INTEGER NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_EXECUTED BIGINT DEFAULT -1 NOT NULL,
  BOOL_ENABLED BOOLEAN DEFAULT 't' NOT NULL,
  BOOL_WORKING BOOLEAN DEFAULT 'f' NOT NULL,
  INT_LEVELS INTEGER NOT NULL,
  JSON_LEVELS VARCHAR(131072) DEFAULT '{}' NOT NULL
);

CREATE UNIQUE INDEX DYHI_PK_FOLDER ON ZORROA.DYHI(PK_FOLDER);

---
---

CREATE TABLE ZORROA.JOB(
  PK_JOB BIGSERIAL PRIMARY KEY,
  STR_NAME VARCHAR(255) NOT NULL,
  INT_TYPE SMALLINT DEFAULT 0 NOT NULL,
  INT_STATE SMALLINT DEFAULT 0 NOT NULL,
  INT_USER_CREATED INTEGER NOT NULL,
  TIME_STARTED BIGINT NOT NULL,
  STR_ROOT_PATH VARCHAR(255) NOT NULL,
  JSON_ARGS VARCHAR(131072) DEFAULT '{}' NOT NULL,
  JSON_ENV VARCHAR(131072) DEFAULT '{}' NOT NULL
);


CREATE INDEX JOB_NAME_IDX ON ZORROA.JOB(STR_NAME);
CREATE INDEX JOB_TYPE_IDX ON ZORROA.JOB(INT_TYPE);
CREATE INDEX JOB_STATE_IDX ON ZORROA.JOB(INT_STATE);

---
---

CREATE TABLE ZORROA.EXPORT_FILE (
  PK_EXPORT_FILE BIGSERIAL PRIMARY KEY,
  PK_JOB BIGINT NOT NULL REFERENCES ZORROA.JOB (PK_JOB),
  STR_NAME TEXT NOT NULL,
  STR_PATH TEXT NOT NULL,
  STR_MIME_TYPE TEXT NOT NULL,
  INT_SIZE BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX EXPORT_FILE_PK_JOB_STR_NAME_UIDX ON ZORROA.EXPORT_FILE(PK_JOB, STR_NAME);

---
---

CREATE TABLE ZORROA.USERS(
  PK_USER SERIAL PRIMARY KEY,
  STR_USERNAME VARCHAR(255) NOT NULL,
  STR_PASSWORD VARCHAR(100) NOT NULL,
  STR_EMAIL VARCHAR(255) NOT NULL,
  STR_FIRSTNAME VARCHAR(255),
  STR_LASTNAME VARCHAR(255),
  BOOL_ENABLED BOOLEAN NOT NULL,
  HMAC_KEY UUID,
  STR_SOURCE VARCHAR(16) DEFAULT 'local' NOT NULL,
  PK_PERMISSION INTEGER NOT NULL,
  PK_FOLDER INTEGER NOT NULL,
  BOOL_RESET_PASS BOOLEAN DEFAULT 'f' NOT NULL,
  STR_RESET_PASS_TOKEN VARCHAR(64),
  TIME_RESET_PASS BIGINT DEFAULT 0 NOT NULL,
  JSON_SETTINGS VARCHAR(131072) DEFAULT '{}' NOT NULL
);

CREATE UNIQUE INDEX USER_STR_USERNAME_IDX ON ZORROA.USERS(STR_USERNAME);
CREATE UNIQUE INDEX USER_STR_EMAIL_IDX ON ZORROA.USERS(STR_EMAIL);

---
---

CREATE TABLE ZORROA.USER_PERMISSION(
  PK_PERMISSION INTEGER NOT NULL,
  PK_USER INTEGER NOT NULL,
  BOOL_IMMUTABLE BOOLEAN DEFAULT 'f' NOT NULL
);

ALTER TABLE ZORROA.USER_PERMISSION ADD CONSTRAINT CONSTRAINT_FE866C PRIMARY KEY(PK_USER, PK_PERMISSION);
CREATE INDEX USER_PERMISSION_PK_PERMISSION_IDX ON ZORROA.USER_PERMISSION(PK_PERMISSION);

---
---
CREATE TABLE ZORROA.PRESET(
  PK_PRESET SERIAL PRIMARY KEY,
  STR_NAME VARCHAR(128) NOT NULL,
  JSON_SETTINGS TEXT NOT NULL
);

---
---

CREATE TABLE ZORROA.JOB_COUNT(
  PK_JOB INTEGER PRIMARY KEY,
  INT_TASK_TOTAL_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_COMPLETED_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_STATE_WAITING_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_STATE_QUEUED_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_STATE_RUNNING_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_STATE_SUCCESS_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_STATE_FAILURE_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_TASK_STATE_SKIPPED_COUNT INTEGER DEFAULT 0 NOT NULL,
  TIME_UPDATED BIGINT DEFAULT ZORROA.CURRENT_TIMESTAMP() NOT NULL
);

---
---

CREATE TABLE ZORROA.MIGRATION(
  PK_MIGRATION SERIAL PRIMARY KEY,
  INT_TYPE INTEGER NOT NULL,
  STR_NAME VARCHAR(128) NOT NULL,
  STR_PATH VARCHAR(255) NOT NULL,
  INT_VERSION INTEGER DEFAULT 1 NOT NULL,
  TIME_APPLIED BIGINT DEFAULT -1 NOT NULL,
  BOOL_ENABLED BOOLEAN DEFAULT 't' NOT NULL,
  INT_PATCH INTEGER DEFAULT 0 NOT NULL
);

INSERT INTO ZORROA.MIGRATION(INT_TYPE, STR_NAME, STR_PATH, INT_VERSION, TIME_APPLIED, BOOL_ENABLED, INT_PATCH) VALUES
  (0, 'archivist', 'classpath:/db/mappings/archivist/*.json', 10, 1454449960000, TRUE, 0),
  (0, 'analyst', 'classpath:/db/mappings/analyst/*.json', 1, 1454449960000, TRUE, 0),
  (0, 'notes', 'classpath:/db/mappings/notes/*.json', 1, 1454449960000, TRUE, 0),
  (0, 'job_logs', 'classpath:/db/mappings/job_logs/*.json', 1, 1454449960000, TRUE, 0),
  (0, 'user_logs', 'classpath:/db/mappings/user_logs/*.json', 1, 1454449960000, TRUE, 0);

---
---

CREATE TABLE ZORROA.FOLDER_ACL(
  PK_PERMISSION INTEGER NOT NULL,
  PK_FOLDER INTEGER NOT NULL,
  INT_ACCESS INTEGER DEFAULT 0 NOT NULL
);

ALTER TABLE ZORROA.FOLDER_ACL ADD CONSTRAINT CONSTRAINT_EBA PRIMARY KEY(PK_FOLDER, PK_PERMISSION);
CREATE INDEX FOLDER_ACL_PK_PERMISSION_IDX ON ZORROA.FOLDER_ACL(PK_PERMISSION);

---
---

CREATE TABLE ZORROA.PRESET_PERMISSION(
  PK_PERMISSION INTEGER NOT NULL,
  PK_PRESET INTEGER NOT NULL
);
ALTER TABLE ZORROA.PRESET_PERMISSION ADD CONSTRAINT CONSTRAINT_F763 PRIMARY KEY(PK_PERMISSION, PK_PRESET);
CREATE INDEX PRESET_PERMISSION_PK_PRESET ON ZORROA.PRESET_PERMISSION(PK_PRESET);

---
---

CREATE TABLE ZORROA.JOB_STAT(
  PK_JOB INTEGER PRIMARY KEY,
  INT_ASSET_TOTAL_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_CREATE_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_ERROR_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_WARNING_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_UPDATE_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_REPLACE_COUNT INTEGER DEFAULT 0 NOT NULL
);
---
---

CREATE TABLE ZORROA.TASK_STAT(
  PK_TASK INTEGER PRIMARY KEY,
  PK_JOB INTEGER NOT NULL,
  INT_ASSET_TOTAL_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_CREATE_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_ERROR_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_WARNING_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_UPDATE_COUNT INTEGER DEFAULT 0 NOT NULL,
  INT_ASSET_REPLACE_COUNT INTEGER DEFAULT 0 NOT NULL
);

CREATE INDEX TASK_STAT_PK_JOB_IDX ON ZORROA.TASK_STAT(PK_JOB);

---
---

CREATE TABLE ZORROA.PROCESSOR(
  PK_PROCESSOR SERIAL PRIMARY KEY,
  PK_PLUGIN INTEGER NOT NULL,
  STR_NAME VARCHAR(255) NOT NULL,
  STR_SHORT_NAME VARCHAR(255) NOT NULL,
  STR_MODULE VARCHAR(255) NOT NULL,
  STR_DESCRIPTION VARCHAR(255) NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_MODIFIED BIGINT NOT NULL,
  JSON_DISPLAY VARCHAR(131072) DEFAULT '{}' NOT NULL,
  JSON_FILTERS VARCHAR(131072) DEFAULT '[]' NOT NULL,
  JSON_FILE_TYPES VARCHAR(131072) DEFAULT '[]' NOT NULL,
  INT_TYPE INTEGER  DEFAULT 0 NOT NULL
);

CREATE INDEX PROCESSOR_PK_PLUGIN_IDX ON ZORROA.PROCESSOR(PK_PLUGIN);
CREATE UNIQUE INDEX PROCESSOR_STR_NAME_IDX ON ZORROA.PROCESSOR(STR_NAME);

---
---

CREATE TABLE ZORROA.FOLDER_TRASH(
  PK_FOLDER_TRASH SERIAL PRIMARY KEY,
  PK_FOLDER INTEGER NOT NULL,
  PK_PARENT INTEGER NOT NULL,
  STR_OPID VARCHAR(36) NOT NULL,
  STR_NAME VARCHAR(128) NOT NULL,
  BOOL_RECURSIVE BOOLEAN NOT NULL,
  USER_CREATED INTEGER NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_MODIFIED BIGINT NOT NULL,
  USER_DELETED INTEGER NOT NULL,
  TIME_DELETED BIGINT NOT NULL,
  BOOL_PRIMARY BOOLEAN NOT NULL,
  INT_ORDER INTEGER NOT NULL,
  JSON_ATTRS VARCHAR(131072) DEFAULT '{}' NOT NULL,
  JSON_ACL VARCHAR(131072),
  JSON_SEARCH VARCHAR(131072)
);

CREATE INDEX FOLDER_TRASH_PK_FOLDER ON ZORROA.FOLDER_TRASH(PK_FOLDER);
CREATE INDEX FOLDER_TRASH_PK_PARENT ON ZORROA.FOLDER_TRASH(PK_PARENT);
CREATE INDEX FOLDER_TRASH_STR_OPID ON ZORROA.FOLDER_TRASH(STR_OPID);

---
---

CREATE TABLE ZORROA.SHARED_LINK(
  PK_SHARED_LINK SERIAL PRIMARY KEY,
  PK_USER INTEGER NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_EXPIRED BIGINT NOT NULL,
  JSON_STATE TEXT,
  JSON_USERS TEXT
);

CREATE INDEX SHARED_LINK_PK_USER_IDX ON ZORROA.SHARED_LINK(PK_USER);

---
---

CREATE TABLE ZORROA.PERMISSION(
  PK_PERMISSION SERIAL PRIMARY KEY,
  STR_NAME VARCHAR(128) NOT NULL,
  STR_DESCRIPTION VARCHAR(255) NOT NULL,
  STR_TYPE VARCHAR(32) NOT NULL,
  BOOL_IMMUTABLE BOOLEAN DEFAULT 'f' NOT NULL,
  BOOL_USER_ASSIGNABLE BOOLEAN DEFAULT 't' NOT NULL,
  BOOL_OBJ_ASSIGNABLE BOOLEAN DEFAULT 't' NOT NULL,
  STR_AUTHORITY VARCHAR(128) NOT NULL
);

CREATE UNIQUE INDEX PERMISSION_NAME_AND_TYPE_UNIQ_IDX ON ZORROA.PERMISSION(STR_NAME, STR_TYPE);

---
---

CREATE TABLE ZORROA.TASK(
  PK_TASK SERIAL PRIMARY KEY,
  PK_PARENT INTEGER,
  PK_JOB INTEGER NOT NULL,
  INT_STATE SMALLINT DEFAULT 0 NOT NULL,
  TIME_STARTED BIGINT DEFAULT -1 NOT NULL,
  TIME_STOPPED BIGINT DEFAULT -1 NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  TIME_STATE_CHANGE BIGINT NOT NULL,
  INT_ORDER SMALLINT DEFAULT 0 NOT NULL,
  INT_EXIT_STATUS SMALLINT DEFAULT -1 NOT NULL,
  STR_HOST VARCHAR(255),
  PK_DEPEND_PARENT INTEGER,
  INT_DEPEND_COUNT INTEGER DEFAULT 0 NOT NULL,
  STR_NAME VARCHAR(128) NOT NULL,
  TIME_PING BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX TASK_PARENT_IDX ON ZORROA.TASK(PK_PARENT);
CREATE INDEX TASK_PK_JOB_IDX ON ZORROA.TASK(PK_JOB);
CREATE INDEX TASK_STATE_IDX ON ZORROA.TASK(INT_STATE);
CREATE INDEX TASK_ORDER_IDX ON ZORROA.TASK(INT_ORDER);

---
---

CREATE TABLE ZORROA.FIELD_HIDE (
  PK_FIELD VARCHAR(128) PRIMARY KEY,
  BOOL_MANUAL BOOLEAN DEFAULT 'f' NOT NULL
);

---
---

CREATE TABLE ZORROA.JBLOB (
  PK_JBLOB BIGSERIAL PRIMARY KEY,
  INT_VERSION BIGINT NOT NULL DEFAULT 1,
  STR_APP TEXT NOT NULL,
  STR_FEATURE TEXT NOT NULL,
  STR_NAME TEXT NOT NULL,
  JSON_DATA TEXT NOT NULL,
  USER_CREATED INTEGER NOT NULL,
  TIME_CREATED BIGINT NOT NULL,
  USER_MODIFIED INTEGER NOT NULL,
  TIME_MODIFIED BIGINT NOT NULL
);

CREATE UNIQUE INDEX jblob_uniq_idx ON jblob (str_app, str_feature, str_name);

CREATE TABLE ZORROA.JBLOB_ACL (
  PK_PERMISSION INTEGER NOT NULL REFERENCES ZORROA.PERMISSION(PK_PERMISSION) ON DELETE CASCADE,
  PK_JBLOB INTEGER NOT NULL REFERENCES ZORROA.JBLOB(PK_JBLOB) ON DELETE CASCADE,
  INT_ACCESS INTEGER,
  PRIMARY KEY (PK_JBLOB, PK_PERMISSION)
);

ALTER TABLE ZORROA.JBLOB_ACL ADD CONSTRAINT CONSTRAINT PRIMARY KEY(PK_JBLOB, PK_PERMISSION);
CREATE INDEX JBLOB_ACL_PK_PERMISSION_IDX ON ZORRA.JBLOB_ACL (PK_PERMISSION);

----
----

---CREATE FORCE TRIGGER ZORROA.TRIGGER_UPDATE_FOLDER_CHILD_COUNT AFTER UPDATE ON ZORROA.FOLDER FOR EACH ROW QUEUE 1024 CALL "com.zorroa.archivist.repository.triggers.TriggerUpdateFolderChildCount";
---CREATE FORCE TRIGGER ZORROA.TRIGGER_INCREMENT_FOLDER_CHILD_COUNT AFTER INSERT ON ZORROA.FOLDER FOR EACH ROW QUEUE 1024 CALL "com.zorroa.archivist.repository.triggers.TriggerIncrementFolderChildCount";
---CREATE FORCE TRIGGER ZORROA.TRIGGER_DECREMENT_FOLDER_CHILD_COUNT AFTER DELETE ON ZORROA.FOLDER FOR EACH ROW QUEUE 1024 CALL "com.zorroa.archivist.repository.triggers.TriggerDecrementFolderChildCount";


INSERT INTO ZORROA.PERMISSION(PK_PERMISSION, STR_NAME, STR_DESCRIPTION, STR_TYPE, BOOL_IMMUTABLE, BOOL_USER_ASSIGNABLE, BOOL_OBJ_ASSIGNABLE, STR_AUTHORITY) VALUES
  (10, 'admin', 'The Admin user', 'user', TRUE, FALSE, TRUE, 'user::admin'),
  (6, 'administrator', 'Superuser, can do and access everything', 'group', TRUE, TRUE, TRUE, 'group::administrator'),
  (7, 'manager', 'Can manage users and permissions', 'group', TRUE, TRUE, TRUE, 'group::manager'),
  (12, 'developer', 'Can manage and create pipelines', 'group', TRUE, TRUE, TRUE, 'group::developer'),
  (13, 'share', 'Can share restricted assets with any groups or user.', 'group', TRUE, TRUE, FALSE, 'group::share'),
  (14, 'everyone', 'A standard user of the system.  All users get this permission.', 'group', TRUE, TRUE, TRUE, 'group::everyone'),
  (15, 'export', 'User can export all data', 'group', TRUE, TRUE, FALSE, 'group::export'),
  (16, 'readAll', 'User can read all data', 'group', TRUE, TRUE, FALSE, 'group::readAll'),
  (17, 'writeAll', 'User can write all data', 'group', TRUE, TRUE, FALSE, 'group::writeAll');

ALTER SEQUENCE zorroa.permission_pk_permission_seq RESTART WITH 18;

INSERT INTO ZORROA.FOLDER(PK_FOLDER, PK_PARENT, STR_NAME, USER_CREATED, TIME_CREATED, USER_MODIFIED, TIME_MODIFIED, BOOL_RECURSIVE, PK_DYHI, BOOL_DYHI_ROOT, STR_DYHI_FIELD, INT_CHILD_COUNT, BOOL_TAX_ROOT, JSON_ATTRS, JSON_SEARCH) VALUES
  (5, 1, 'admin', 1, 1450709321000, 1, 1450709321000, TRUE, NULL, FALSE, NULL, 0, FALSE, '{}', NULL),
  (1, 0, 'Users', 1, 1450709321000, 1, 1450709321000, FALSE, NULL, FALSE, NULL, 1, FALSE, '{}', NULL),
  (0, NULL, '/', 1, 1450709321000, 1, 1450709321000, FALSE, NULL, FALSE, NULL, 2, FALSE, '{}', '{}'),
  (14, 0, 'Library', 1, 1450709321000, 1, 1450709321000, FALSE, NULL, FALSE, NULL, 0, FALSE, '{}', '{}');

ALTER SEQUENCE zorroa.folder_pk_folder_seq RESTART WITH 15;

INSERT INTO ZORROA.FOLDER_ACL(PK_PERMISSION, PK_FOLDER, INT_ACCESS) VALUES
  (10, 5, 3),
  (7, 0, 3),
  (14, 0, 1),
  (14, 1, 1),
  (7, 14, 3),
  (14, 14, 1);

---
---

INSERT INTO ZORROA.USERS(PK_USER, STR_USERNAME, STR_PASSWORD, STR_EMAIL, STR_FIRSTNAME, STR_LASTNAME, BOOL_ENABLED, HMAC_KEY, STR_SOURCE, PK_PERMISSION, PK_FOLDER, BOOL_RESET_PASS, STR_RESET_PASS_TOKEN, TIME_RESET_PASS, JSON_SETTINGS) VALUES
  (1, 'admin', '$2a$10$26Ekb4MDeUdz75G4V2u6geSuI1Hn4jrHUvZafK5M2iHdz5s9oLGyK', 'admin@zorroa.com', 'Joe', 'Admin', TRUE, '00000000-0000-0000-0000-000000000000', 'local', 10, 5, TRUE, NULL, 0, '{}');

ALTER SEQUENCE zorroa.users_pk_user_seq RESTART WITH 2;

INSERT INTO ZORROA.USER_PERMISSION(PK_PERMISSION, PK_USER, BOOL_IMMUTABLE) VALUES
  (7, 1, FALSE),
  (10, 1, TRUE),
  (12, 1, FALSE),
  (13, 1, FALSE),
  (14, 1, FALSE),
  (6, 1, FALSE);

---
---
---

ALTER TABLE ZORROA.USER_PERMISSION ADD CONSTRAINT CONSTRAINT_FE866 FOREIGN KEY(PK_USER) REFERENCES ZORROA.USERS(PK_USER) ON DELETE CASCADE ;
ALTER TABLE ZORROA.FOLDER_ACL ADD CONSTRAINT CONSTRAINT_EB FOREIGN KEY(PK_FOLDER) REFERENCES ZORROA.FOLDER(PK_FOLDER) ON DELETE CASCADE ;
ALTER TABLE ZORROA.PROCESSOR ADD CONSTRAINT CONSTRAINT_64D FOREIGN KEY(PK_PLUGIN) REFERENCES ZORROA.PLUGIN(PK_PLUGIN) ON DELETE CASCADE ;
ALTER TABLE ZORROA.TASK ADD CONSTRAINT CONSTRAINT_272D FOREIGN KEY(PK_JOB) REFERENCES ZORROA.JOB(PK_JOB) ON DELETE CASCADE ;
ALTER TABLE ZORROA.FOLDER ADD CONSTRAINT CONSTRAINT_7BF FOREIGN KEY(PK_PARENT) REFERENCES ZORROA.FOLDER(PK_FOLDER) ;
ALTER TABLE ZORROA.DYHI ADD CONSTRAINT CONSTRAINT_204 FOREIGN KEY(PK_FOLDER) REFERENCES ZORROA.FOLDER(PK_FOLDER) ON DELETE CASCADE ;
ALTER TABLE ZORROA.FOLDER_ACL ADD CONSTRAINT CONSTRAINT_E FOREIGN KEY(PK_PERMISSION) REFERENCES ZORROA.PERMISSION(PK_PERMISSION) ON DELETE CASCADE ;
ALTER TABLE ZORROA.PRESET_PERMISSION ADD CONSTRAINT CONSTRAINT_F76 FOREIGN KEY(PK_PRESET) REFERENCES ZORROA.PRESET(PK_PRESET) ON DELETE CASCADE ;
ALTER TABLE ZORROA.USERS ADD CONSTRAINT CONSTRAINT_27E FOREIGN KEY(PK_PERMISSION) REFERENCES ZORROA.PERMISSION(PK_PERMISSION) ;
ALTER TABLE ZORROA.TAXONOMY ADD CONSTRAINT CONSTRAINT_1F0 FOREIGN KEY(PK_FOLDER) REFERENCES ZORROA.FOLDER(PK_FOLDER) ;
ALTER TABLE ZORROA.TASK ADD CONSTRAINT CONSTRAINT_272 FOREIGN KEY(PK_PARENT) REFERENCES ZORROA.TASK(PK_TASK) ON DELETE SET NULL ;
ALTER TABLE ZORROA.SHARED_LINK ADD CONSTRAINT CONSTRAINT_A1C4 FOREIGN KEY(PK_USER) REFERENCES ZORROA.USERS(PK_USER) ON DELETE CASCADE ;
ALTER TABLE ZORROA.USERS ADD CONSTRAINT CONSTRAINT_27E3 FOREIGN KEY(PK_FOLDER) REFERENCES ZORROA.FOLDER(PK_FOLDER) ;
ALTER TABLE ZORROA.PRESET_PERMISSION ADD CONSTRAINT CONSTRAINT_F7 FOREIGN KEY(PK_PERMISSION) REFERENCES ZORROA.PERMISSION(PK_PERMISSION) ON DELETE CASCADE ;
ALTER TABLE ZORROA.USER_PERMISSION ADD CONSTRAINT CONSTRAINT_FE86 FOREIGN KEY(PK_PERMISSION) REFERENCES ZORROA.PERMISSION(PK_PERMISSION) ON DELETE CASCADE ;

---
---
---


CREATE OR REPLACE FUNCTION ZORROA.TRIGGER_UPDATE_FOLDER_CHILD_COUNT() RETURNS TRIGGER AS $$
BEGIN
  IF OLD.pk_parent > NEW.pk_parent THEN
    UPDATE zorroa.folder SET int_child_count=int_child_count -1 WHERE pk_folder=OLD.pk_parent;
    UPDATE zorroa.folder SET int_child_count=int_child_count +1 WHERE pk_folder=NEW.pk_parent;
  ELSE
    UPDATE zorroa.folder SET int_child_count=int_child_count +1 WHERE pk_folder=NEW.pk_parent;
    UPDATE zorroa.folder SET int_child_count=int_child_count -1 WHERE pk_folder=OLD.pk_parent;
  END IF;
  RETURN NEW;
END
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_after_folder_update AFTER UPDATE ON ZORROA.FOLDER
  FOR EACH ROW WHEN (OLD.PK_PARENT != NEW.PK_PARENT)
  EXECUTE PROCEDURE TRIGGER_UPDATE_FOLDER_CHILD_COUNT();


CREATE OR REPLACE FUNCTION ZORROA.TRIGGER_INCREMENT_FOLDER_CHILD_COUNT() RETURNS TRIGGER AS $$
BEGIN
  UPDATE zorroa.folder SET int_child_count=int_child_count +1 WHERE pk_folder=NEW.pk_parent;
RETURN NEW;
END
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_after_folder_insert AFTER INSERT ON ZORROA.FOLDER
FOR EACH ROW EXECUTE PROCEDURE TRIGGER_INCREMENT_FOLDER_CHILD_COUNT();


CREATE OR REPLACE FUNCTION ZORROA.TRIGGER_DECREMENT_FOLDER_CHILD_COUNT() RETURNS TRIGGER AS $$
BEGIN
  UPDATE zorroa.folder SET int_child_count=int_child_count -1 WHERE pk_folder=OLD.pk_parent;
  RETURN OLD;
END
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_after_folder_delete AFTER DELETE ON ZORROA.FOLDER
FOR EACH ROW EXECUTE PROCEDURE TRIGGER_DECREMENT_FOLDER_CHILD_COUNT();

CREATE OR REPLACE FUNCTION ZORROA.TRIGGER_UPDATE_JOB_STATE() RETURNS TRIGGER AS $$
BEGIN
  IF NEW.int_task_state_success_count + NEW.int_task_state_failure_count + NEW.int_task_state_skipped_count = NEW.int_task_total_count THEN
    UPDATE job SET int_state=2 WHERE pk_job=NEW.pk_job AND int_state=0;
  ELSE
    UPDATE job SET int_state=0 WHERE pk_job=NEW.pk_job AND int_state=2;
  END IF;

  RETURN NEW;
END
$$
LANGUAGE plpgsql;

CREATE TRIGGER trig_update_job_state AFTER UPDATE ON ZORROA.JOB_COUNT
FOR EACH ROW EXECUTE PROCEDURE TRIGGER_UPDATE_JOB_STATE();

