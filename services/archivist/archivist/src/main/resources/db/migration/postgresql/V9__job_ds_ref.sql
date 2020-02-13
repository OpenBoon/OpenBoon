
ALTER TABLE job DROP CONSTRAINT job_pk_datasource_fkey;
ALTER TABLE job ADD CONSTRAINT job_pk_datasource_fkey
        FOREIGN KEY (pk_datasource)
        REFERENCES datasource(pk_datasource)
        ON DELETE SET NULL;
