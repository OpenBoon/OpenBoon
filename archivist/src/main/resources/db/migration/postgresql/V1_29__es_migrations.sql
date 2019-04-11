
--- This table holds routes to ES indexes.
CREATE TABLE index_route (
  pk_index_route UUID PRIMARY KEY,
  str_url TEXT NOT NULL,
  str_index TEXT NOT NULL,
  str_mapping_type TEXT NOT NULL,
  int_mapping_major_ver SMALLINT NOT NULL,
  int_mapping_minor_ver INTEGER NOT NULL,
  int_replicas SMAllINT NOT NULL,
  int_shards SMAllINT NOT NULL,
  bool_closed BOOLEAN NOT NULL,
  bool_default_pool BOOLEAN NOT NULL,
  bool_use_rkey BOOLEAN NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  int_mapping_error_ver INTEGER NOT NULL DEFAULT -1
);

CREATE UNIQUE INDEX index_route_idx_uniq ON index_route (str_url, str_index);

--- This is the default route.
INSERT INTO index_route VALUES (
  '00000000-0000-0000-0000-000000000000'::uuid,
  'http://es:9200',
  'assets_v12',
  'asset',
  12,
  0,
  2,
  5,
  'f',
  't',
  't',
  1554919886000,
  1554919886000
);

--- All organizations need a route.
ALTER TABLE organization ADD COLUMN pk_index_route UUID REFERENCES index_route(pk_index_route);
UPDATE organization SET pk_index_route='00000000-0000-0000-0000-000000000000'::uuid;
ALTER TABLE organization ALTER COLUMN pk_index_route SET NOT NULL;

CREATE INDEX organization_pk_index_route_idx ON organization (pk_index_route);

DROP TABLE migration;
