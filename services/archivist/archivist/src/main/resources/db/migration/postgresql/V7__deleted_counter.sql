-- Project Quota
ALTER TABLE project_quota ADD float_deleted_video_seconds numeric(14,2) NOT NULL DEFAULT 0;
ALTER TABLE project_quota ADD int_deleted_page_count bigint NOT NULL DEFAULT 0;

-- Project Quota Time Series
ALTER TABLE project_quota_time_series ADD float_deleted_video_seconds numeric(14,2) NOT NULL DEFAULT 0;
ALTER TABLE project_quota_time_series ADD int_deleted_video_file_count bigint NOT NULL DEFAULT 0;
ALTER TABLE project_quota_time_series ADD int_deleted_document_file_count bigint NOT NULL DEFAULT 0;
ALTER TABLE project_quota_time_series ADD int_deleted_image_file_count bigint NOT NULL DEFAULT 0;
ALTER TABLE project_quota_time_series ADD int_deleted_video_clip_count bigint NOT NULL DEFAULT 0;
ALTER TABLE project_quota_time_series ADD int_deleted_page_count bigint NOT NULL DEFAULT 0;

