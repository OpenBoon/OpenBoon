
/**
 * Warnings occur when a processor fails on an asset, however the critical processors
 * succeeded.
 */
ALTER TABLE ingest ADD int_warning_count INTEGER NOT NULL DEFAULT 0;
