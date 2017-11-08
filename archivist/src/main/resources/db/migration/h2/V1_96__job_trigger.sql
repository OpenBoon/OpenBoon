
CREATE TRIGGER trigger_update_job_state
AFTER UPDATE
  ON job_count
FOR EACH ROW CALL "com.zorroa.archivist.repository.triggers.TriggerUpdateJobState";
