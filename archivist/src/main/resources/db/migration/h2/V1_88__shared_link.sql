
CREATE TABLE shared_link (
  pk_shared_link INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_user INTEGER NOT NULL REFERENCES user(pk_user) ON DELETE CASCADE,
  time_created BIGINT NOT NULL,
  time_expired BIGINT NOT NULL,
  json_state TEXT,
  json_users TEXT
);

CREATE INDEX shared_link_pk_user_idx ON shared_link(pk_user);



