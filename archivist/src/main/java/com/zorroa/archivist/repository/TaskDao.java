package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 7/11/16.
 */
public interface TaskDao {
    ZpsScript create(ZpsScript script);

    boolean setHost(ZpsTask task, String host);

    boolean setExitStatus(ZpsTask task, int exitStatus);

    boolean setState(ZpsTask script, TaskState value, TaskState expect);

    int decrementDependCount(ZpsTask task);

    int incrementDependCount(ZpsTask task);

    boolean createParentDepend(ZpsTask child);

    List<ZpsScript> getWaiting(int limit);

    Set<TaskState> STOPPERS = Sets.newEnumSet(ImmutableList.of(
            TaskState.Failure, TaskState.Success), TaskState.class);

    Set<TaskState> STARTERS = Sets.newEnumSet(ImmutableList.of(
            TaskState.Running), TaskState.class);

    Set<TaskState> RESET = Sets.newEnumSet(ImmutableList.of(
            TaskState.Waiting), TaskState.class);

    List<ZpsTask> getOrphanTasks(int limit, long duration, TimeUnit unit);

    PagedList<Task> getAll(int job, Paging page);

    Task get(int id);

    long countByJob(int job);
}
