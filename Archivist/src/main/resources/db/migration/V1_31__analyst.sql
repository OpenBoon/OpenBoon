CREATE TABLE analyst (
  pk_analyst INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_url VARCHAR(128) NOT NULL,
  int_state INTEGER NOT NULL DEFAULT 1,
  bool_locked BOOLEAN NOT NULL DEFAULT 0,
  bool_data BOOLEAN NOT NULL,
  int_threads_total INTEGER NOT NULL,
  int_threads_active INTEGER NOT NULL,
  int_queue_size INTEGER NOT NULL,
  int_process_success INTEGER NOT NULL,
  int_process_failed INTEGER NOT NULL,
  time_created BIGINT NOT NULL,
  time_updated BIGINT NOT NULL,
  json_ingestor_classes TEXT NOT NULL
);

CREATE UNIQUE INDEX analyst_str_name_uniq_idx ON analyst (str_url);
