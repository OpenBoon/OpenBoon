
/*
 * Exports are offline by default.
 */
ALTER TABLE export_output ADD bool_offline BOOLEAN NOT NULL DEFAULT 1;

/**
 * The last time an export output came online.
 */
ALTER TABLE export_output ADD time_online BIGINT NOT NULL DEFAULT -1;

/**
 * The time the export output went offline.
 */
ALTER TABLE export_output ADD time_offline BIGINT NOT NULL DEFAULT -1;
