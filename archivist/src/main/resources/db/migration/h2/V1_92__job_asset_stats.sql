

ALTER TABLE job_stat ALTER COLUMN int_frame_total_count RENAME TO int_asset_total_count;
ALTER TABLE job_stat ALTER COLUMN int_frame_success_count RENAME TO int_asset_create_count;
ALTER TABLE job_stat ALTER COLUMN int_frame_error_count RENAME TO int_asset_error_count;
ALTER TABLE job_stat ALTER COLUMN int_frame_warning_count RENAME TO int_asset_warning_count;
ALTER TABLE job_stat ADD COLUMN int_asset_update_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE job_stat ADD COLUMN int_asset_replace_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE task_stat ALTER COLUMN int_frame_total_count RENAME TO int_asset_total_count;
ALTER TABLE task_stat ALTER COLUMN int_frame_success_count RENAME TO int_asset_create_count;
ALTER TABLE task_stat ALTER COLUMN int_frame_error_count RENAME TO int_asset_error_count;
ALTER TABLE task_stat ALTER COLUMN int_frame_warning_count RENAME TO int_asset_warning_count;
ALTER TABLE task_stat ADD COLUMN int_asset_update_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE task_stat ADD COLUMN int_asset_replace_count INTEGER NOT NULL DEFAULT 0;
