
/**
 * The timestamp for the last state change of a job.
 */
ALTER TABLE job_count ADD COLUMN time_updated BIGINT NOT NULL DEFAULT CURRENT_TIMESTAMP;

/**
 * Don't really have a stop time anymore.
 */
ALTER TABLE job DROP COLUMN time_stopped;



