
ALTER TABLE model ADD COLUMN json_search_deploy JSONB NOT NULL DEFAULT '{"query": {"match_all":  {}}}';

--- Move the face rec back to #2
UPDATE model SET int_type=2 WHERE int_type=4;