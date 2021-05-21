CREATE TABLE dataset
(
    pk_dataset     UUID PRIMARY KEY,
    pk_project     UUID   NOT NULL REFERENCES project (pk_project) ON DELETE CASCADE,
    str_name       TEXT   NOT NULL,
    int_type       SMALLINT NOT NULL,
    str_descr      TEXT NOT NULL,
    int_model_count INTEGER NOT NULL DEFAULT 0,
    time_created   BIGINT NOT NULL,
    time_modified  BIGINT NOT NULL,
    actor_created  TEXT   NOT NULL,
    actor_modified TEXT   NOT NULL
);

CREATE INDEX dataset_pk_project_idx ON dataset (pk_project);
CREATE UNIQUE INDEX dataset_str_name_pk_project_idx ON dataset (str_name, pk_project);

INSERT INTO dataset (pk_dataset, pk_project, str_name, int_type, str_descr, time_created, time_modified, actor_created,
                     actor_modified)
SELECT pk_model,
       pk_project,
       str_name,
       int_type,
       'A dataset for model training' as str_descr,
       time_created,
       time_modified,
       actor_created,
       actor_modified
FROM model;

ALTER TABLE model ADD COLUMN pk_dataset UUID REFERENCES dataset ON DELETE SET NULL;
CREATE INDEX model_pk_dataset_idx ON model (pk_dataset);


UPDATE model SET pk_dataset = pk_model;


CREATE FUNCTION after_model_update() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.pk_dataset != NEW.pk_dataset THEN
        IF NEW.pk_dataset IS NOT NULL THEN
            UPDATE dataset SET int_model_count=int_model_count+1 WHERE pk_dataset = NEW.pk_dataset;
        END IF;
        IF OLD.pk_dataset IS NOT NULL THEN
            UPDATE dataset SET int_model_count=int_model_count-1 WHERE pk_dataset = OLD.pk_dataset;
        END IF;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trig_after_model_update AFTER UPDATE ON model FOR EACH ROW EXECUTE PROCEDURE after_model_update();

---
CREATE FUNCTION after_model_insert() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.pk_dataset IS NOT NULL THEN
        UPDATE dataset SET int_model_count=int_model_count+1 WHERE pk_dataset = NEW.pk_dataset;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trig_after_model_insert AFTER INSERT ON model FOR EACH ROW EXECUTE PROCEDURE after_model_insert();
