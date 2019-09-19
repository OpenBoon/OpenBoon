
UPDATE permission SET str_type='assets' WHERE str_type='asset';

ALTER TABLE organization ALTER COLUMN pk_index_route DROP NOT NULL;