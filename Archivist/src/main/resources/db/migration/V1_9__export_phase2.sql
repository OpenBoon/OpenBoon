ALTER TABLE export_output ADD str_file_ext VARCHAR(16) NOT NULL;

ALTER TABLE export ADD int_asset_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE export ADD int_total_file_size BIGINT NOT NULL DEFAULT 0;

ALTER TABLE export_output ADD int_file_size BIGINT NOT NULL DEFAULT 0;
