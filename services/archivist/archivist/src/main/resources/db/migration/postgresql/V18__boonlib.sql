
CREATE TABLE boonlib(
    pk_boonlib UUID PRIMARY KEY,
    name TEXT NOT NULL,
    entity SMALLINT NOT NULL,
    entity_type TEXT NOT NULL,
    descr TEXT NOT NULL,
    state SMALLINT NOT NULL,
    time_created BIGINT NOT NULL,
    time_modified BIGINT NOT NULL,
    actor_created TEXT NOT NULL,
    actor_modified TEXT NOT NULL
);

CREATE UNIQUE INDEX boonlib_str_name_uidx ON boonlib (name);
