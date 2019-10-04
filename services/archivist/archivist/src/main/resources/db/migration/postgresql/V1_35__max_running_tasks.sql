ALTER TABLE job_count ADD COLUMN int_max_running_tasks INTEGER NOT NULL DEFAULT 1024;

ALTER TABLE job_count ADD CONSTRAINT job_count_max_running_check CHECK
    (int_task_state_1 + int_task_state_5  <= int_max_running_tasks);