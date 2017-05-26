package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.TaskSpec;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.archivist.domain.TaskId;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 7/11/16.
 */
public interface TaskDao {

    Set<TaskState> STOPPERS = Sets.newEnumSet(ImmutableList.of(
            TaskState.Skipped, TaskState.Failure, TaskState.Success), TaskState.class);

    Set<TaskState> STARTERS = Sets.newEnumSet(ImmutableList.of(
            TaskState.Running), TaskState.class);

    Set<TaskState> RESET = Sets.newEnumSet(ImmutableList.of(
            TaskState.Waiting), TaskState.class);

    Task create(TaskSpec spec);

    boolean setHost(TaskId task, String host);

    boolean setExitStatus(TaskId task, int exitStatus);

    boolean incrementStats(int id, int success, int errors, int warnings);

    TaskState getState(TaskId task, boolean forUpdate);

    TaskStartT getExecutableTask(int id);

    boolean setState(TaskId script, TaskState value, TaskState ... expect);

    int decrementDependCount(TaskId task);

    int incrementDependCount(TaskId task);

    boolean createParentDepend(TaskId child);

    List<TaskStartT> getWaiting(int limit);

    List<Task> getOrphanTasks(int limit, long duration, TimeUnit unit);

    PagedList<Task> getAll(int job, Pager page);

    List<Task> getAll(int job, DaoFilter filter);

    Task get(int id);

    long countByJob(int job);

    /**
     * Update's a tasks ping time.
     * @param taskIds
     */
    int updatePingTime(List<Integer> taskIds);
}
