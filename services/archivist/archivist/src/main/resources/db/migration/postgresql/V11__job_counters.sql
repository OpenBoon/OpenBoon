
CREATE FUNCTION trigger_increment_job_error_counts() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.bool_fatal THEN
        UPDATE job_stat SET int_asset_error_count=int_asset_error_count+1 WHERE pk_job=NEW.pk_job;
        UPDATE task_stat SET int_asset_error_count=int_asset_error_count+1 WHERE pk_task=NEW.pk_task;
    ELSE
        UPDATE job_stat SET int_asset_warning_count=int_asset_warning_count+1 WHERE pk_job=NEW.pk_job;
        UPDATE task_stat SET int_asset_warning_count=int_asset_warning_count+1 WHERE pk_task=NEW.pk_task;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trig_after_task_error_insert
    AFTER INSERT ON task_error FOR EACH ROW EXECUTE PROCEDURE trigger_increment_job_error_counts();


CREATE FUNCTION trigger_decrement_job_error_counts() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.bool_fatal THEN
        UPDATE job_stat SET int_asset_error_count=int_asset_error_count-1 WHERE pk_job=OLD.pk_job;
        UPDATE task_stat SET int_asset_error_count=int_asset_error_count-1 WHERE pk_task=OLD.pk_task;
    ELSE
        UPDATE job_stat SET int_asset_warning_count=int_asset_warning_count-1 WHERE pk_job=OLD.pk_job;
        UPDATE task_stat SET int_asset_warning_count=int_asset_warning_count-1 WHERE pk_task=OLD.pk_task;
    END IF;
    RETURN OLD;
END
$$;


CREATE TRIGGER trig_after_task_error_delete
    AFTER DELETE ON task_error FOR EACH ROW EXECUTE PROCEDURE trigger_decrement_job_error_counts();


