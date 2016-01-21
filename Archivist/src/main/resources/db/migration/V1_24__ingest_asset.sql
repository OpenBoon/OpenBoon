/**
 * The idea here is before he processor starts to work on a path, it adds it to this table.
 * When its done, it removes it, If the server crashes there should be some data left in this table, unfortunately
 * there might be more than just the asset that crashed, but to be safe we'll skip all of them.
 */
CREATE TABLE ingest_skip (
    pk_ingest_skip INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
    pk_ingest INTEGER NOT NULL REFERENCES ingest (pk_ingest) ON DELETE CASCADE,
    str_path VARCHAR(2048) NOT NULL,
    time_created BIGINT NOT NULL
);
CREATE UNIQUE INDEX ingest_skip_unique_idx ON ingest_skip (pk_ingest, str_path);
