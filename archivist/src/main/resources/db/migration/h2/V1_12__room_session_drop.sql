ALTER TABLE room DROP COLUMN pk_session;

DROP TABLE map_session_to_room;

CREATE TABLE map_session_to_room (
  pk_session BIGINT NOT NULL REFERENCES session (pk_session) ON DELETE CASCADE,
  pk_room BIGINT NOT NULL REFERENCES room (pk_room) ON DELETE CASCADE,
  PRIMARY KEY (pk_room, pk_session)
);

CREATE INDEX map_session_to_room_pk_session_idx ON map_session_to_room (pk_session);

