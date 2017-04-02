DELETE FROM folder WHERE str_name='Trash';

ALTER TABLE folder ADD COLUMN int_child_count INTEGER NOT NULL DEFAULT 0;
UPDATE folder SET int_child_count=(SELECT COUNT(1) FROM folder f WHERE f.pk_parent = folder.pk_folder);

CREATE TRIGGER trigger_decrement_folder_child_count
AFTER DELETE
ON folder
FOR EACH ROW CALL "com.zorroa.archivist.repository.triggers.TriggerDecrementFolderChildCount";

CREATE TRIGGER trigger_increment_folder_child_count
AFTER INSERT
ON folder
FOR EACH ROW CALL "com.zorroa.archivist.repository.triggers.TriggerIncrementFolderChildCount";

CREATE TRIGGER trigger_update_folder_child_count
AFTER UPDATE
ON folder
FOR EACH ROW CALL "com.zorroa.archivist.repository.triggers.TriggerUpdateFolderChildCount";
