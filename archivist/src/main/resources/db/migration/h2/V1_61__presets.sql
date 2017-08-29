
CREATE TABLE preset  (
  pk_preset INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(128) NOT NULL,
  json_settings TEXT NOT NULL
);

CREATE TABLE preset_permission (
  pk_permission INTEGER NOT NULL REFERENCES permission (pk_permission) ON DELETE CASCADE,
  pk_preset INTEGER NOT NULL REFERENCES preset (pk_preset) ON DELETE CASCADE,
  PRIMARY KEY (pk_permission, pk_preset)
);

CREATE INDEX preset_permission_pk_preset ON preset_permission (pk_preset);

/**
 * A json doc describing the users search configuration.
 */
ALTER TABLE user ADD json_settings TEXT NOT NULL DEFAULT '{}';
