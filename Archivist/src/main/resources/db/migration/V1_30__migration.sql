
/**
 * The migration table is for versioning other components besides the SQL DB
 * that Flyway supports, for example ElasticSearch.
 */
CREATE TABLE migration (
  pk_migration INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  int_type INTEGER NOT NULL,
  str_name VARCHAR(128) NOT NULL,
  str_path VARCHAR(255) NOT NULL,
  int_version INTEGER NOT NULL DEFAULT 1,
  time_applied BIGINT NOT NULL DEFAULT -1,
  bool_enabled BOOLEAN NOT NULL DEFAULT 1
);

INSERT INTO migration (int_type, str_name, str_path, int_version, time_applied)
  VALUES (0, 'elastic', 'elastic-mapping.json', 1, 1454449960000);

