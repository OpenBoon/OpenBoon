
ALTER TABLE model RENAME COLUMN json_search_deploy TO json_apply_search;

UPDATE model SET str_file_id=REPLACE(str_file_id, '/model/', '/__TAG__/');
