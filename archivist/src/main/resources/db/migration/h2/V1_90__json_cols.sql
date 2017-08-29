DROP TABLE room;
DROP TABLE session;
DROP TABLE map_session_to_room;

ALTER TABLE user ADD COLUMN json_settings2 VARCHAR(131072);
UPDATE user SET json_settings2=json_settings;
ALTER TABLE user DROP COLUMN json_settings;
ALTER TABLE user ALTER COLUMN json_settings2 RENAME TO json_settings;
ALTER TABLE user ALTER COLUMN json_settings SET DEFAULT '{}';
ALTER TABLE user ALTER COLUMN json_settings SET NOT NULL;

ALTER TABLE job ADD COLUMN json_args2 VARCHAR(131072);
UPDATE job SET json_args2=json_args;
ALTER TABLE job DROP COLUMN json_args;
ALTER TABLE job ALTER COLUMN json_args2 RENAME TO json_args;
ALTER TABLE job ALTER COLUMN json_args SET DEFAULT '{}';
ALTER TABLE job ALTER COLUMN json_args SET NOT NULL;

ALTER TABLE job ADD COLUMN json_env2 VARCHAR(131072);
UPDATE job SET json_env2=json_env;
ALTER TABLE job DROP COLUMN json_env;
ALTER TABLE job ALTER COLUMN json_env2 RENAME TO json_env;
ALTER TABLE job ALTER COLUMN json_env SET DEFAULT '{}';
ALTER TABLE job ALTER COLUMN json_env SET NOT NULL;

ALTER TABLE dyhi ADD COLUMN json_levels2 VARCHAR(131072);
UPDATE dyhi SET json_levels2=json_levels;
ALTER TABLE dyhi DROP COLUMN json_levels;
ALTER TABLE dyhi ALTER COLUMN json_levels2 RENAME TO json_levels;
ALTER TABLE dyhi ALTER COLUMN json_levels SET DEFAULT '{}';
ALTER TABLE dyhi ALTER COLUMN json_levels SET NOT NULL;

ALTER TABLE processor ADD COLUMN json_display2 VARCHAR(131072);
UPDATE processor SET json_display2=json_display;
ALTER TABLE processor DROP COLUMN json_display;
ALTER TABLE processor ALTER COLUMN json_display2 RENAME TO json_display;
ALTER TABLE processor ALTER COLUMN json_display SET DEFAULT '{}';
ALTER TABLE processor ALTER COLUMN json_display SET NOT NULL;

ALTER TABLE processor ADD COLUMN json_filters2 VARCHAR(131072);
UPDATE processor SET json_filters2=json_filters;
ALTER TABLE processor DROP COLUMN json_filters;
ALTER TABLE processor ALTER COLUMN json_filters2 RENAME TO json_filters;
ALTER TABLE processor ALTER COLUMN json_filters SET DEFAULT '[]';
ALTER TABLE processor ALTER COLUMN json_filters SET NOT NULL;

ALTER TABLE processor ADD COLUMN json_file_types2 VARCHAR(131072);
UPDATE processor SET json_file_types2=json_file_types;
ALTER TABLE processor DROP COLUMN json_file_types;
ALTER TABLE processor ALTER COLUMN json_file_types2 RENAME TO json_file_types;
ALTER TABLE processor ALTER COLUMN json_file_types SET DEFAULT '[]';
ALTER TABLE processor ALTER COLUMN json_file_types SET NOT NULL;

ALTER TABLE folder_trash ADD COLUMN json_attrs2 VARCHAR(131072);
UPDATE folder_trash SET json_attrs2=json_attrs;
ALTER TABLE folder_trash DROP COLUMN json_attrs;
ALTER TABLE folder_trash ALTER COLUMN json_attrs2 RENAME TO json_attrs;
ALTER TABLE folder_trash ALTER COLUMN json_attrs SET DEFAULT '{}';
ALTER TABLE folder_trash ALTER COLUMN json_attrs SET NOT NULL;

ALTER TABLE folder_trash ADD COLUMN json_acl2 VARCHAR(131072);
UPDATE folder_trash SET json_acl2=json_acl;
ALTER TABLE folder_trash DROP COLUMN json_acl;
ALTER TABLE folder_trash ALTER COLUMN json_acl2 RENAME TO json_acl;

ALTER TABLE folder_trash ADD COLUMN json_search2 VARCHAR(131072);
UPDATE folder_trash SET json_search2=json_search;
ALTER TABLE folder_trash DROP COLUMN json_search;
ALTER TABLE folder_trash ALTER COLUMN json_search2 RENAME TO json_search;

ALTER TABLE folder ADD COLUMN json_attrs2 VARCHAR(131072);
UPDATE folder SET json_attrs2=json_attrs;
ALTER TABLE folder DROP COLUMN json_attrs;
ALTER TABLE folder ALTER COLUMN json_attrs2 RENAME TO json_attrs;
ALTER TABLE folder ALTER COLUMN json_attrs SET DEFAULT '{}';
ALTER TABLE folder ALTER COLUMN json_attrs SET NOT NULL;

ALTER TABLE folder ADD COLUMN json_search2 VARCHAR(131072);
UPDATE folder SET json_search2=json_search;
ALTER TABLE folder DROP COLUMN json_search;
ALTER TABLE folder ALTER COLUMN json_search2 RENAME TO json_search;

ALTER TABLE command ADD COLUMN json_args2 VARCHAR(131072);
UPDATE command SET json_args2=json_args;
ALTER TABLE command DROP COLUMN json_args;
ALTER TABLE command ALTER COLUMN json_args2 RENAME TO json_args;
ALTER TABLE command ALTER COLUMN json_args SET DEFAULT '[]';
ALTER TABLE command ALTER COLUMN json_args SET NOT NULL;
