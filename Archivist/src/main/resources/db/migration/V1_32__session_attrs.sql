/**
 * The last time an export output came online.
 */
ALTER TABLE session ADD json_attrs TEXT NOT NULL DEFAULT '{}';
