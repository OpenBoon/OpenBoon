package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.util.Json;

import java.util.Set;

/**
 * Created by chambers on 8/8/16.
 */
public class TaskFilter extends DaoFilter {

    public Boolean all;
    public Set<Integer> tasks;
    public Set<TaskState> states;
    public Integer jobId;

    public TaskFilter() {
        sortMap = ImmutableMap.of(
                "tasks", "pk_task",
                "states", "int_state");
    }

    @JsonIgnore
    public void build() {

        if (all != null) {
            if (all) {
                return;
            }
        }

        logger.info("values222: {}", Json.serializeToString(values));

        if (JdbcUtils.isValid(jobId)) {
            where.add("task.pk_job=?");
            values.add(jobId);
            logger.info("values1: {}", Json.serializeToString(values));
        }

        if (JdbcUtils.isValid(tasks)) {
            where.add(JdbcUtils.in("task.pk_task", tasks.size()));
            values.addAll(tasks);
            logger.info("values2: {}", Json.serializeToString(values));
        }

        if (JdbcUtils.isValid(states)) {
            where.add(JdbcUtils.in("task.int_state", states.size()));
            for (TaskState s: states) {
                values.add(s.ordinal());
            }
            logger.info("values3: {}", Json.serializeToString(values));
        }
    }

    public Set<Integer> getTasks() {
        return tasks;
    }

    public TaskFilter setTasks(Set<Integer> tasks) {
        this.tasks = tasks;
        return this;
    }

    public Set<TaskState> getStates() {
        return states;
    }

    public TaskFilter setStates(Set<TaskState> states) {
        this.states = states;
        return this;
    }

    public Boolean getAll() {
        return all;
    }

    public TaskFilter setAll(Boolean all) {
        this.all = all;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public TaskFilter setJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }
}
