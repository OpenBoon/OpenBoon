package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;
import com.zorroa.common.domain.TaskState;

import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 8/8/16.
 */
public class TaskFilter extends DaoFilter {

    private static final Map<String,String> sortMap = ImmutableMap.<String, String>builder()
                .put("taskId", "pk_task")
                .put("parentId", "pk_parent")
                .put("state", "int_state")
                .put("name", "str_name")
                .put("host", "str_host")
                .put("timeStarted", "time_started")
                .put("timeStopped", "time_stopped")
                .build();

    public Boolean all;
    public Set<Integer> tasks;
    public Set<TaskState> states;
    public Integer jobId;

    public TaskFilter() { }

    @JsonIgnore
    public void build() {

        if (all != null) {
            if (all) {
                return;
            }
        }

        if (JdbcUtils.isValid(jobId)) {
            addToWhere("task.pk_job=?");
            addToValues(jobId);
        }

        if (JdbcUtils.isValid(tasks)) {
            addToWhere(JdbcUtils.in("task.pk_task", tasks.size()));
            addToValues(tasks);
        }

        if (JdbcUtils.isValid(states)) {
            addToWhere(JdbcUtils.in("task.int_state", states.size()));
            for (TaskState s: states) {
                addToValues(s.ordinal());
            }
        }
    }

    @Override
    public Map<String, String> getSortMap() {
        return sortMap;
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
