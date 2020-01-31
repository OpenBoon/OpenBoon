ALTER TABLE datasource DROP COLUMN pk_pipeline;

CREATE TABLE x_module_datasource
(
    pk_x_module_datasource     uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    pk_module                  uuid NOT NULL REFERENCES module (pk_module) ON DELETE CASCADE,
    pk_datasource              uuid NOT NULL REFERENCES datasource (pk_datasource) ON DELETE CASCADE
);

CREATE UNIQUE INDEX x_module_datasource_uniq_idx ON x_module_datasource (pk_datasource, pk_module);
CREATE INDEX x_module_datasource_pk_module_uniq_idx ON x_module_datasource (pk_module);
