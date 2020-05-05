
DROP TABLE project_quota_time_series;

CREATE TABLE project_quota_time_series
(
    pk_project              UUID           NOT NULL REFERENCES project (pk_project) ON DELETE CASCADE,
    int_entry               INTEGER        NOT NULL,
    time                    BIGINT         NOT NULL,
    int_video_file_count    BIGINT         NOT NULL DEFAULT 0,
    int_document_file_count BIGINT         NOT NULL DEFAULT 0,
    int_image_file_count    BIGINT         NOT NULL DEFAULT 0,
    float_video_seconds     DECIMAL(14, 2) NOT NULL DEFAULT 0,
    int_page_count          BIGINT         NOT NULL DEFAULT 0,
    int_video_clip_count    BIGINT         NOT NULL DEFAULT 0,
    constraint project_stats_time_series_pk primary key (pk_project, int_entry)
);

CREATE UNIQUE INDEX
    project_quota_time_series_entry_uniq_date_idx ON project_quota_time_series (time, pk_project);