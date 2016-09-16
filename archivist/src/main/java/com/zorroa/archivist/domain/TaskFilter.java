package com.zorroa.archivist.domain;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;

import java.util.Set;

/**
 * Created by chambers on 8/8/16.
 */
public class TaskFilter extends DaoFilter {

    public Boolean all;
    public Set<Integer> tasks;
    public Set<TaskState> states;

    public TaskFilter() {
    }

    public void build() {

        if (all != null) {
            if (all) {
                return;
            }
        }

        if (JdbcUtils.isValid(tasks)) {
            where.add(JdbcUtils.in("task.pk_task", tasks.size()));
            values.addAll(tasks);
        }

        if (JdbcUtils.isValid(states)) {
            where.add(JdbcUtils.in("task.int_state", states.size()));
            for (TaskState s: states) {
                values.add(s.ordinal());
            }
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
}
