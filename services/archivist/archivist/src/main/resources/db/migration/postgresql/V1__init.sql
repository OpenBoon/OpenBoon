---
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;

--
-- Name: after_task_insert(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION after_task_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  UPDATE job_count SET int_task_total_count=int_task_total_count+1,
    int_task_state_0=int_task_state_0+1 WHERE pk_job=new.pk_job;
  RETURN NEW;
END
$$;

--
-- Name: after_task_state_change(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION after_task_state_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
DECLARE
    old_state_col VARCHAR;
    new_state_col VARCHAR;
    tx_time BIGINT;
BEGIN
  SELECT EXTRACT(EPOCH FROM NOW()) * 1000 INTO tx_time;
  old_state_col := 'int_task_state_' || old.int_state::text;
  new_state_col := 'int_task_state_' || new.int_state::text;
  EXECUTE 'UPDATE job_count SET ' || old_state_col || '=' || old_state_col || '-1,'
    || new_state_col || '=' || new_state_col || '+1, time_updated=$1 WHERE job_count.pk_job = $2' USING tx_time, new.pk_job;
  RETURN NEW;
END
$_$;

--
-- Name: bitand(integer, integer); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION bitand(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
BEGIN
  RETURN $1 & $2;
END;
$_$;

--
-- Name: current_time_millis(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION current_time_millis() RETURNS bigint
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN (extract(epoch from clock_timestamp()) * 1000)::BIGINT;
end;
$$;

--
-- Name: trigger_decrement_job_error_counts(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

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

--
-- Name: trigger_increment_job_error_counts(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

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

--
-- Name: trigger_update_job_state(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION trigger_update_job_state() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  IF NEW.int_task_state_success_count + NEW.int_task_state_failure_count + NEW.int_task_state_skipped_count = NEW.int_task_total_count THEN
    UPDATE job set int_state=2 where pk_job=NEW.pk_job AND int_state=0;
  ELSE
    UPDATE job set int_state=0 where pk_job=NEW.pk_job AND int_state=2;
  END IF;

  RETURN NEW;
END
$$;

--
-- Name: analyst; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE analyst (
    pk_analyst uuid NOT NULL,
    pk_task uuid,
    int_state smallint DEFAULT 0 NOT NULL,
    int_lock_state smallint DEFAULT 0 NOT NULL,
    time_created bigint NOT NULL,
    time_ping bigint NOT NULL,
    str_endpoint text NOT NULL,
    int_total_ram integer NOT NULL,
    int_free_ram integer NOT NULL,
    flt_load double precision NOT NULL,
    int_free_disk integer DEFAULT 1024 NOT NULL,
    str_version text DEFAULT '0.41.0-1547158402'::text NOT NULL
);

--
-- Name: credentials; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE credentials (
    pk_credentials uuid NOT NULL,
    pk_project uuid NOT NULL,
    str_name character varying(64) NOT NULL,
    int_type smallint NOT NULL,
    str_blob text,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL
);

--
-- Name: data_set; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE data_set (
    pk_data_set uuid NOT NULL,
    pk_project uuid,
    str_name text,
    int_type smallint,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL
);

--
-- Name: datasource; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE datasource (
    pk_datasource uuid NOT NULL,
    pk_project uuid NOT NULL,
    str_name character varying(64) NOT NULL,
    str_uri text NOT NULL,
    str_file_types text,
    str_analysis text,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL
);

--
-- Name: flyway_schema_history; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE index_cluster (
    pk_index_cluster uuid NOT NULL,
    str_url text NOT NULL,
    int_state smallint NOT NULL,
    bool_autopool boolean NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    time_ping bigint NOT NULL,
    json_attrs jsonb DEFAULT '{}'::jsonb NOT NULL
);

--
-- Name: index_route; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE index_route (
    pk_index_route uuid NOT NULL,
    pk_index_cluster uuid NOT NULL,
    pk_project uuid,
    str_index text NOT NULL,
    int_state smallint NOT NULL,
    str_mapping_type text NOT NULL,
    int_mapping_major_ver smallint NOT NULL,
    int_mapping_minor_ver integer NOT NULL,
    int_replicas smallint NOT NULL,
    int_shards smallint NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    int_mapping_error_ver integer DEFAULT '-1'::integer NOT NULL
);

--
-- Name: job; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE job (
    pk_job uuid NOT NULL,
    pk_project uuid NOT NULL,
    pk_datasource uuid,
    str_name text NOT NULL,
    int_type smallint DEFAULT 0 NOT NULL,
    int_state smallint DEFAULT 0 NOT NULL,
    time_started bigint NOT NULL,
    json_args text DEFAULT '{}'::text NOT NULL,
    json_env text DEFAULT '{}'::text NOT NULL,
    int_priority smallint DEFAULT 0 NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    time_stopped bigint DEFAULT '-1'::integer NOT NULL,
    time_pause_expired bigint DEFAULT '-1'::integer NOT NULL,
    bool_paused boolean DEFAULT false NOT NULL
);

--
-- Name: job_count; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE job_count (
    pk_job uuid NOT NULL,
    int_task_total_count integer DEFAULT 0 NOT NULL,
    int_task_completed_count integer DEFAULT 0 NOT NULL,
    int_task_state_0 integer DEFAULT 0 NOT NULL,
    int_task_state_5 integer DEFAULT 0 NOT NULL,
    int_task_state_1 integer DEFAULT 0 NOT NULL,
    int_task_state_2 integer DEFAULT 0 NOT NULL,
    int_task_state_3 integer DEFAULT 0 NOT NULL,
    int_task_state_4 integer DEFAULT 0 NOT NULL,
    time_updated bigint DEFAULT current_time_millis() NOT NULL,
    int_max_running_tasks integer DEFAULT 1024 NOT NULL,
    CONSTRAINT job_count_max_running_check CHECK (((int_task_state_1 + int_task_state_5) <= int_max_running_tasks))
);

--
-- Name: job_stat; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE job_stat (
    pk_job uuid NOT NULL,
    int_asset_total_count integer DEFAULT 0 NOT NULL,
    int_asset_create_count integer DEFAULT 0 NOT NULL,
    int_asset_error_count integer DEFAULT 0 NOT NULL,
    int_asset_warning_count integer DEFAULT 0 NOT NULL,
    int_asset_update_count integer DEFAULT 0 NOT NULL,
    int_asset_replace_count integer DEFAULT 0 NOT NULL
);

--
-- Name: model; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE model (
    pk_model uuid NOT NULL,
    pk_project uuid NOT NULL,
    pk_data_set uuid NOT NULL,
    str_name text NOT NULL,
    int_type smallint NOT NULL,
    str_file_id text NOT NULL,
    str_job_name text NOT NULL,
    bool_trained boolean DEFAULT false NOT NULL,
    int_version integer DEFAULT 1 NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    time_trained bigint DEFAULT '-1'::integer NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL
);

--
-- Name: module; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE module (
    pk_module uuid NOT NULL,
    pk_project uuid,
    str_name text NOT NULL,
    int_version integer DEFAULT 1 NOT NULL,
    str_description text NOT NULL,
    json_ops jsonb DEFAULT '{}'::jsonb NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL,
    str_provider text DEFAULT 'Zorroa'::text NOT NULL,
    str_category text DEFAULT 'Visual Intelligence'::text NOT NULL,
    str_supported_media text DEFAULT 'Images,Video,Documents'::text NOT NULL,
    str_type text DEFAULT 'Machine Learning'::text NOT NULL
);

--
-- Name: pipeline; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE pipeline (
    pk_pipeline uuid NOT NULL,
    pk_project uuid NOT NULL,
    int_mode smallint NOT NULL,
    str_name text NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL,
    json_processors jsonb DEFAULT '[]'::jsonb NOT NULL
);

--
-- Name: plugin; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE plugin (
    pk_plugin uuid NOT NULL,
    str_name text NOT NULL,
    str_lang text NOT NULL,
    str_description text NOT NULL,
    str_version text NOT NULL,
    str_publisher text NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    str_md5 text
);

--
-- Name: processor; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE processor (
    pk_processor uuid NOT NULL,
    pk_project uuid,
    str_name text NOT NULL,
    str_file text NOT NULL,
    str_type text NOT NULL,
    str_description text DEFAULT ''::text NOT NULL,
    time_updated bigint NOT NULL,
    json_display jsonb DEFAULT '[]'::jsonb NOT NULL,
    list_file_types text[] DEFAULT '{}'::text[] NOT NULL,
    fti_keywords tsvector NOT NULL
);

--
-- Name: project; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE project (
    pk_project uuid NOT NULL,
    str_name character varying(64) NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    actor_created text NOT NULL,
    actor_modified text NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    int_tier int2 NOT NULL DEFAULT 0
);

--
-- Name: project_quota; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE project_quota (
    pk_project uuid NOT NULL,
    int_max_video_seconds bigint DEFAULT 86400 NOT NULL,
    int_max_page_count bigint DEFAULT 10000 NOT NULL,
    float_video_seconds numeric(14,2) DEFAULT 0 NOT NULL,
    int_page_count bigint DEFAULT 0 NOT NULL
);


ALTER TABLE project_quota OWNER TO zorroa;

--
-- Name: project_quota_time_series; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE project_quota_time_series (
    pk_project uuid NOT NULL,
    int_entry integer NOT NULL,
    "time" bigint NOT NULL,
    int_video_file_count bigint DEFAULT 0 NOT NULL,
    int_document_file_count bigint DEFAULT 0 NOT NULL,
    int_image_file_count bigint DEFAULT 0 NOT NULL,
    float_video_seconds numeric(14,2) DEFAULT 0 NOT NULL,
    int_page_count bigint DEFAULT 0 NOT NULL,
    int_video_clip_count bigint DEFAULT 0 NOT NULL
);


ALTER TABLE project_quota_time_series OWNER TO zorroa;

--
-- Name: project_settings; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE project_settings (
    pk_project_settings uuid NOT NULL,
    pk_project uuid NOT NULL,
    pk_pipeline_default uuid NOT NULL,
    pk_index_route_default uuid NOT NULL
);


ALTER TABLE project_settings OWNER TO zorroa;

--
-- Name: task; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE task (
    pk_task uuid NOT NULL,
    pk_parent uuid,
    pk_job uuid NOT NULL,
    int_state smallint DEFAULT 0 NOT NULL,
    time_started bigint DEFAULT '-1'::integer NOT NULL,
    time_stopped bigint DEFAULT '-1'::integer NOT NULL,
    time_created bigint NOT NULL,
    time_state_change bigint NOT NULL,
    time_ping bigint DEFAULT 0 NOT NULL,
    int_order smallint DEFAULT 0 NOT NULL,
    int_exit_status smallint DEFAULT '-1'::integer NOT NULL,
    str_host text,
    str_name text NOT NULL,
    time_modified bigint DEFAULT '1536693258000'::bigint NOT NULL,
    json_script text DEFAULT '{}'::text NOT NULL,
    int_run_count integer DEFAULT 0 NOT NULL,
    int_ping_count integer DEFAULT 0 NOT NULL,
    int_progress smallint DEFAULT 0 NOT NULL,
    str_status text DEFAULT ''::text NOT NULL
);


ALTER TABLE task OWNER TO zorroa;

--
-- Name: task_error; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE task_error (
    pk_task_error uuid NOT NULL,
    pk_task uuid NOT NULL,
    pk_job uuid NOT NULL,
    asset_id text,
    str_message text,
    str_path text,
    str_processor text,
    str_endpoint text,
    str_extension text,
    time_created bigint NOT NULL,
    bool_fatal boolean NOT NULL,
    str_phase text NOT NULL,
    json_stack_trace jsonb,
    fti_keywords tsvector NOT NULL
);

--
-- Name: task_stat; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE task_stat (
    pk_task uuid NOT NULL,
    pk_job uuid NOT NULL,
    int_asset_total_count integer DEFAULT 0 NOT NULL,
    int_asset_create_count integer DEFAULT 0 NOT NULL,
    int_asset_error_count integer DEFAULT 0 NOT NULL,
    int_asset_warning_count integer DEFAULT 0 NOT NULL,
    int_asset_update_count integer DEFAULT 0 NOT NULL,
    int_asset_replace_count integer DEFAULT 0 NOT NULL
);

--
-- Name: x_credentials_datasource; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE x_credentials_datasource (
    pk_x_credentials_datasource uuid NOT NULL,
    pk_credentials uuid NOT NULL,
    pk_datasource uuid NOT NULL,
    int_type smallint NOT NULL
);

--
-- Name: x_credentials_job; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE x_credentials_job (
    pk_x_credentials_job uuid NOT NULL,
    pk_credentials uuid NOT NULL,
    pk_job uuid NOT NULL,
    int_type smallint NOT NULL
);

--
-- Name: x_module_datasource; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE x_module_datasource (
    pk_x_module_datasource BIGSERIAL NOT NULL,
    pk_module uuid NOT NULL,
    pk_datasource uuid NOT NULL
);

--
-- Name: x_module_pipeline; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE x_module_pipeline (
    pk_x_module_pipeline BIGSERIAL NOT NULL,
    pk_module uuid NOT NULL,
    pk_pipeline uuid NOT NULL
);

--
-- Name: analyst analyst_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY analyst
    ADD CONSTRAINT analyst_pkey PRIMARY KEY (pk_analyst);


--
-- Name: credentials credentials_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY credentials
    ADD CONSTRAINT credentials_pkey PRIMARY KEY (pk_credentials);


--
-- Name: data_set data_set_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY data_set
    ADD CONSTRAINT data_set_pkey PRIMARY KEY (pk_data_set);


--
-- Name: datasource datasource_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY datasource
    ADD CONSTRAINT datasource_pkey PRIMARY KEY (pk_datasource);

--
-- Name: index_cluster index_cluster_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY index_cluster
    ADD CONSTRAINT index_cluster_pkey PRIMARY KEY (pk_index_cluster);


--
-- Name: index_route index_route_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY index_route
    ADD CONSTRAINT index_route_pkey PRIMARY KEY (pk_index_route);


--
-- Name: job_count job_count_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY job_count
    ADD CONSTRAINT job_count_pkey PRIMARY KEY (pk_job);


--
-- Name: job job_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pkey PRIMARY KEY (pk_job);


--
-- Name: job_stat job_stat_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY job_stat
    ADD CONSTRAINT job_stat_pkey PRIMARY KEY (pk_job);


--
-- Name: model model_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY model
    ADD CONSTRAINT model_pkey PRIMARY KEY (pk_model);


--
-- Name: module module_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY module
    ADD CONSTRAINT module_pkey PRIMARY KEY (pk_module);


--
-- Name: pipeline pipeline_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY pipeline
    ADD CONSTRAINT pipeline_pkey PRIMARY KEY (pk_pipeline);


--
-- Name: plugin plugin_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY plugin
    ADD CONSTRAINT plugin_pkey PRIMARY KEY (pk_plugin);


--
-- Name: processor processor_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY processor
    ADD CONSTRAINT processor_pkey PRIMARY KEY (pk_processor);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project
    ADD CONSTRAINT project_pkey PRIMARY KEY (pk_project);


--
-- Name: project_quota project_quota_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_quota
    ADD CONSTRAINT project_quota_pkey PRIMARY KEY (pk_project);


--
-- Name: project_settings project_settings_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_settings
    ADD CONSTRAINT project_settings_pkey PRIMARY KEY (pk_project_settings);


--
-- Name: project_quota_time_series project_stats_time_series_pk; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_quota_time_series
    ADD CONSTRAINT project_stats_time_series_pk PRIMARY KEY (pk_project, int_entry);


--
-- Name: task_error task_error_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task_error
    ADD CONSTRAINT task_error_pkey PRIMARY KEY (pk_task_error);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task
    ADD CONSTRAINT task_pkey PRIMARY KEY (pk_task);


--
-- Name: x_credentials_datasource x_credentials_datasource_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_credentials_datasource
    ADD CONSTRAINT x_credentials_datasource_pkey PRIMARY KEY (pk_x_credentials_datasource);


--
-- Name: x_credentials_job x_credentials_job_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_credentials_job
    ADD CONSTRAINT x_credentials_job_pkey PRIMARY KEY (pk_x_credentials_job);


--
-- Name: x_module_datasource x_module_datasource_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_module_datasource
    ADD CONSTRAINT x_module_datasource_pkey PRIMARY KEY (pk_x_module_datasource);


--
-- Name: x_module_pipeline x_module_pipeline_pkey; Type: CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_module_pipeline
    ADD CONSTRAINT x_module_pipeline_pkey PRIMARY KEY (pk_x_module_pipeline);

--
-- Name: analyst_pk_task_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX analyst_pk_task_idx ON analyst USING btree (pk_task);


--
-- Name: analyst_str_endpoint_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX analyst_str_endpoint_idx ON analyst USING btree (str_endpoint);


--
-- Name: credentials_pk_project_str_name_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX credentials_pk_project_str_name_uniq_idx ON credentials USING btree (pk_project, str_name);


--
-- Name: data_set_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX data_set_uniq_idx ON data_set USING btree (pk_project, str_name);


--
-- Name: datasource_pk_project_str_name_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX datasource_pk_project_str_name_idx ON datasource USING btree (pk_project, str_name);

--
-- Name: index_cluster_idx_uniq; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX index_cluster_idx_uniq ON index_cluster USING btree (str_url);


--
-- Name: index_route_idx_uniq; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX index_route_idx_uniq ON index_route USING btree (pk_index_cluster, str_index);


--
-- Name: job_count_int_task_state_0_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_count_int_task_state_0_idx ON job_count USING btree (int_task_state_0);


--
-- Name: job_int_priority_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_int_priority_idx ON job USING btree (int_priority);


--
-- Name: job_name_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_name_idx ON job USING btree (str_name);


--
-- Name: job_pk_datasource_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_pk_datasource_idx ON job USING btree (pk_datasource);


--
-- Name: job_pk_project_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_pk_project_idx ON job USING btree (pk_project);


--
-- Name: job_state_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_state_idx ON job USING btree (int_state);


--
-- Name: job_time_created_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_time_created_idx ON job USING btree (time_created);


--
-- Name: job_time_started_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_time_started_idx ON job USING btree (time_started);


--
-- Name: job_type_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX job_type_idx ON job USING btree (int_type);


--
-- Name: model_pk_dataset_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX model_pk_dataset_uniq_idx ON model USING btree (pk_data_set, int_type);


--
-- Name: model_pk_project_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX model_pk_project_idx ON model USING btree (pk_project);


--
-- Name: module_str_name_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX module_str_name_idx ON module USING btree (str_name);


--
-- Name: module_uniq_idx1; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX module_uniq_idx1 ON module USING btree (((pk_project IS NULL)), str_name) WHERE (pk_project IS NULL);


--
-- Name: module_uniq_idx2; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX module_uniq_idx2 ON module USING btree (pk_project, str_name);


--
-- Name: pipeline_name_uidx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX pipeline_name_uidx ON pipeline USING btree (pk_project, str_name);


--
-- Name: plugin_str_name_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX plugin_str_name_idx ON plugin USING btree (str_name);


--
-- Name: processor_fti_keywords_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX processor_fti_keywords_idx ON processor USING gin (fti_keywords);


--
-- Name: processor_str_name_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX processor_str_name_idx ON processor USING btree (str_name);


--
-- Name: project_quota_time_series_entry_uniq_date_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX project_quota_time_series_entry_uniq_date_idx ON project_quota_time_series USING btree ("time", pk_project);


--
-- Name: project_settings_pk_index_route_default_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX project_settings_pk_index_route_default_uniq_idx ON project_settings USING btree (pk_index_route_default);


--
-- Name: project_settings_pk_pipeline_default_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX project_settings_pk_pipeline_default_uniq_idx ON project_settings USING btree (pk_pipeline_default);


--
-- Name: project_str_name_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX project_str_name_idx ON project USING btree (str_name);


--
-- Name: task_error_asset_id_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_asset_id_idx ON task_error USING btree (asset_id);


--
-- Name: task_error_fti_keywords_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_fti_keywords_idx ON task_error USING gin (fti_keywords);


--
-- Name: task_error_pk_job_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_pk_job_idx ON task_error USING btree (pk_job);


--
-- Name: task_error_pk_task_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_pk_task_idx ON task_error USING btree (pk_task);


--
-- Name: task_error_str_path_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_str_path_idx ON task_error USING btree (str_path);


--
-- Name: task_error_str_processor_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_str_processor_idx ON task_error USING btree (str_processor);


--
-- Name: task_error_time_created_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_error_time_created_idx ON task_error USING btree (time_created);


--
-- Name: task_order_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_order_idx ON task USING btree (int_order);


--
-- Name: task_parent_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_parent_idx ON task USING btree (pk_parent);


--
-- Name: task_pk_job_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_pk_job_idx ON task USING btree (pk_job);


--
-- Name: task_stat_pk_job_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_stat_pk_job_idx ON task_stat USING btree (pk_job);


--
-- Name: task_state_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_state_idx ON task USING btree (int_state);


--
-- Name: task_time_created_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX task_time_created_idx ON task USING btree (time_created);


--
-- Name: x_credentials_datasource_pk_credentials_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX x_credentials_datasource_pk_credentials_idx ON x_credentials_datasource USING btree (pk_credentials);


--
-- Name: x_credentials_datasource_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX x_credentials_datasource_uniq_idx ON x_credentials_datasource USING btree (pk_datasource, int_type);


--
-- Name: x_credentials_job_pk_credentials_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX x_credentials_job_pk_credentials_idx ON x_credentials_job USING btree (pk_credentials);


--
-- Name: x_credentials_job_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX x_credentials_job_uniq_idx ON x_credentials_job USING btree (pk_job, int_type);


--
-- Name: x_module_datasource_pk_module_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX x_module_datasource_pk_module_uniq_idx ON x_module_datasource USING btree (pk_module);


--
-- Name: x_module_datasource_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX x_module_datasource_uniq_idx ON x_module_datasource USING btree (pk_datasource, pk_module);


--
-- Name: x_module_pipeline_pk_pipeline_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE INDEX x_module_pipeline_pk_pipeline_idx ON x_module_pipeline USING btree (pk_pipeline);


--
-- Name: x_module_pipeline_uniq_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX x_module_pipeline_uniq_idx ON x_module_pipeline USING btree (pk_module, pk_pipeline);


--
-- Name: task_error trig_after_task_error_delete; Type: TRIGGER; Schema: zorroa; Owner: zorroa
--

CREATE TRIGGER trig_after_task_error_delete AFTER DELETE ON task_error FOR EACH ROW EXECUTE PROCEDURE trigger_decrement_job_error_counts();


--
-- Name: task_error trig_after_task_error_insert; Type: TRIGGER; Schema: zorroa; Owner: zorroa
--

CREATE TRIGGER trig_after_task_error_insert AFTER INSERT ON task_error FOR EACH ROW EXECUTE PROCEDURE trigger_increment_job_error_counts();


--
-- Name: task trig_after_task_insert; Type: TRIGGER; Schema: zorroa; Owner: zorroa
--

CREATE TRIGGER trig_after_task_insert AFTER INSERT ON task FOR EACH ROW EXECUTE PROCEDURE after_task_insert();


--
-- Name: task trig_after_task_state_change; Type: TRIGGER; Schema: zorroa; Owner: zorroa
--

CREATE TRIGGER trig_after_task_state_change AFTER UPDATE ON task FOR EACH ROW WHEN ((old.int_state <> new.int_state)) EXECUTE PROCEDURE after_task_state_change();


--
-- Name: credentials credentials_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY credentials
    ADD CONSTRAINT credentials_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project) ON DELETE CASCADE;


--
-- Name: data_set data_set_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY data_set
    ADD CONSTRAINT data_set_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project);


--
-- Name: datasource datasource_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY datasource
    ADD CONSTRAINT datasource_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project);


--
-- Name: index_route index_route_pk_index_cluster_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY index_route
    ADD CONSTRAINT index_route_pk_index_cluster_fkey FOREIGN KEY (pk_index_cluster) REFERENCES index_cluster(pk_index_cluster);


--
-- Name: index_route index_route_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY index_route
    ADD CONSTRAINT index_route_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project);


--
-- Name: job job_pk_datasource_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pk_datasource_fkey FOREIGN KEY (pk_datasource) REFERENCES datasource(pk_datasource) ON DELETE SET NULL;


--
-- Name: job job_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project);


--
-- Name: job_stat job_stat_pk_job_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY job_stat
    ADD CONSTRAINT job_stat_pk_job_fkey FOREIGN KEY (pk_job) REFERENCES job(pk_job) ON DELETE CASCADE;


--
-- Name: model model_pk_data_set_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY model
    ADD CONSTRAINT model_pk_data_set_fkey FOREIGN KEY (pk_data_set) REFERENCES data_set(pk_data_set) ON DELETE CASCADE;


--
-- Name: model model_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY model
    ADD CONSTRAINT model_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project) ON DELETE CASCADE;


--
-- Name: module module_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY module
    ADD CONSTRAINT module_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project) ON DELETE CASCADE;


--
-- Name: pipeline pipeline_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY pipeline
    ADD CONSTRAINT pipeline_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project);


--
-- Name: processor processor_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY processor
    ADD CONSTRAINT processor_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project);


--
-- Name: project_quota_time_series project_quota_time_series_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_quota_time_series
    ADD CONSTRAINT project_quota_time_series_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project) ON DELETE CASCADE;


--
-- Name: project_settings project_settings_pk_index_route_default_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_settings
    ADD CONSTRAINT project_settings_pk_index_route_default_fkey FOREIGN KEY (pk_index_route_default) REFERENCES index_route(pk_index_route);


--
-- Name: project_settings project_settings_pk_pipeline_default_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_settings
    ADD CONSTRAINT project_settings_pk_pipeline_default_fkey FOREIGN KEY (pk_pipeline_default) REFERENCES pipeline(pk_pipeline);


--
-- Name: project_settings project_settings_pk_project_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY project_settings
    ADD CONSTRAINT project_settings_pk_project_fkey FOREIGN KEY (pk_project) REFERENCES project(pk_project) ON DELETE CASCADE;


--
-- Name: task_error task_error_pk_job_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task_error
    ADD CONSTRAINT task_error_pk_job_fkey FOREIGN KEY (pk_job) REFERENCES job(pk_job) ON DELETE CASCADE;


--
-- Name: task_error task_error_pk_task_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task_error
    ADD CONSTRAINT task_error_pk_task_fkey FOREIGN KEY (pk_task) REFERENCES task(pk_task) ON DELETE CASCADE;


--
-- Name: task task_pk_job_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task
    ADD CONSTRAINT task_pk_job_fkey FOREIGN KEY (pk_job) REFERENCES job(pk_job) ON DELETE CASCADE;


--
-- Name: task task_pk_parent_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task
    ADD CONSTRAINT task_pk_parent_fkey FOREIGN KEY (pk_parent) REFERENCES task(pk_task) ON DELETE CASCADE;


--
-- Name: task_stat task_stat_pk_job_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task_stat
    ADD CONSTRAINT task_stat_pk_job_fkey FOREIGN KEY (pk_job) REFERENCES job(pk_job) ON DELETE CASCADE;


--
-- Name: task_stat task_stat_pk_task_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY task_stat
    ADD CONSTRAINT task_stat_pk_task_fkey FOREIGN KEY (pk_task) REFERENCES task(pk_task) ON DELETE CASCADE;


--
-- Name: x_credentials_datasource x_credentials_datasource_pk_credentials_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_credentials_datasource
    ADD CONSTRAINT x_credentials_datasource_pk_credentials_fkey FOREIGN KEY (pk_credentials) REFERENCES credentials(pk_credentials) ON DELETE CASCADE;


--
-- Name: x_credentials_datasource x_credentials_datasource_pk_datasource_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_credentials_datasource
    ADD CONSTRAINT x_credentials_datasource_pk_datasource_fkey FOREIGN KEY (pk_datasource) REFERENCES datasource(pk_datasource) ON DELETE CASCADE;


--
-- Name: x_credentials_job x_credentials_job_pk_credentials_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_credentials_job
    ADD CONSTRAINT x_credentials_job_pk_credentials_fkey FOREIGN KEY (pk_credentials) REFERENCES credentials(pk_credentials) ON DELETE CASCADE;


--
-- Name: x_credentials_job x_credentials_job_pk_job_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_credentials_job
    ADD CONSTRAINT x_credentials_job_pk_job_fkey FOREIGN KEY (pk_job) REFERENCES job(pk_job) ON DELETE CASCADE;


--
-- Name: x_module_datasource x_module_datasource_pk_datasource_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_module_datasource
    ADD CONSTRAINT x_module_datasource_pk_datasource_fkey FOREIGN KEY (pk_datasource) REFERENCES datasource(pk_datasource) ON DELETE CASCADE;


--
-- Name: x_module_datasource x_module_datasource_pk_module_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_module_datasource
    ADD CONSTRAINT x_module_datasource_pk_module_fkey FOREIGN KEY (pk_module) REFERENCES module(pk_module) ON DELETE CASCADE;


--
-- Name: x_module_pipeline x_module_pipeline_pk_module_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_module_pipeline
    ADD CONSTRAINT x_module_pipeline_pk_module_fkey FOREIGN KEY (pk_module) REFERENCES module(pk_module);


--
-- Name: x_module_pipeline x_module_pipeline_pk_pipeline_fkey; Type: FK CONSTRAINT; Schema: zorroa; Owner: zorroa
--

ALTER TABLE ONLY x_module_pipeline
    ADD CONSTRAINT x_module_pipeline_pk_pipeline_fkey FOREIGN KEY (pk_pipeline) REFERENCES pipeline(pk_pipeline) ON DELETE CASCADE;
