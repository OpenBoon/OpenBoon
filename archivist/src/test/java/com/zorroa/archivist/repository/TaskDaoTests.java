package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.JobService;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * Created by chambers on 7/12/16.
 */
public class TaskDaoTests extends AbstractTest {

    @Autowired
    JobService jobService;

    @Autowired
    TaskDao taskDao;

    JobSpec spec;
    Job job;
    TaskSpec tspec;
    Task task;

    @Before
    public void init() {
        spec = new JobSpec();
        spec.setType(PipelineType.Export);
        spec.setName("job");
        job = jobService.launch(spec);

        tspec = new TaskSpec();
        tspec.setName("a task");
        tspec.setScript(new ZpsScript());
        tspec.setJobId(job.getJobId());
        task = jobService.createTask(tspec);
    }

    @Test
    public void testCreate() {
        TaskSpec spec = new TaskSpec();
        spec.setName("task1");
        spec.setScript(new ZpsScript());
        spec.setJobId(job.getJobId());
        Task task1 = taskDao.create(spec);
    }

    @Test
    public void testCreateChild() {
        TaskSpec spec = new TaskSpec();
        spec.setName("parent");
        spec.setScript(new ZpsScript());
        spec.setJobId(job.getJobId());
        Task parent = taskDao.create(spec);

        TaskSpec childSpec = new TaskSpec();
        childSpec.setName("child");
        childSpec.setScript(new ZpsScript());
        childSpec.setJobId(job.getJobId());
        childSpec.setParentTaskId(parent.getTaskId());

        Task child = taskDao.create(childSpec);
        assertEquals(childSpec.getName(), child.getName());
        assertEquals(parent.getTaskId(), child.getParentTaskId());
    }

    @Test
    public void testSetState() {
        assertTrue(taskDao.setState(task, TaskState.Queued, TaskState.Waiting));
        assertFalse(taskDao.setState(task, TaskState.Queued, TaskState.Waiting));

        assertTrue(taskDao.setState(task, TaskState.Running, null));
        assertFalse("Task was already running", taskDao.setState(task, TaskState.Running, null));
    }

    @Test
    public void testSetStateMultipleExpect() {
        assertTrue(taskDao.setState(task, TaskState.Queued, TaskState.Failure, TaskState.Waiting));
        assertTrue(taskDao.setState(task, TaskState.Waiting, TaskState.Running, TaskState.Queued));
        assertFalse(taskDao.setState(task, TaskState.Running, TaskState.Queued, TaskState.Success));
    }

    @Test
    public void getWaiting() {
        assertEquals(1, taskDao.getWaiting(5).size());
    }

    @Test
    public void getAllByFilter() {
        for (int i=0; i<10; i++) {
            TaskSpec spec = new TaskSpec();
            spec.setName("a task");
            spec.setScript(new ZpsScript());
            spec.setJobId(job.getJobId());
            task = jobService.createTask(spec);
        }

        assertEquals(11, taskDao.getAll(job.getJobId(), new TaskFilter()).size());
        assertEquals(1, taskDao.getAll(job.getJobId(),
                new TaskFilter().setTasks(ImmutableSet.of(task.getTaskId()))).size());


        assertEquals(11, taskDao.getAll(job.getJobId(),
                new TaskFilter().setStates(ImmutableSet.of(TaskState.Waiting))).size());
        assertEquals(0, taskDao.getAll(job.getJobId(),
                new TaskFilter().setStates(ImmutableSet.of(TaskState.Running))).size());

        assertTrue(jobService.setTaskState(task, TaskState.Queued, TaskState.Waiting));
        assertEquals(10, taskDao.getAll(job.getJobId(),
                new TaskFilter().setStates(ImmutableSet.of(TaskState.Waiting))).size());
        assertEquals(1, taskDao.getAll(job.getJobId(),
                new TaskFilter().setStates(ImmutableSet.of(TaskState.Queued))).size());
    }


    @Test
    public void getOrphanTasks() {
        List<Task> tasks = taskDao.getOrphanTasks(100, 20, TimeUnit.MINUTES);
        assertEquals(0, tasks.size());

        updateState(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20), TaskState.Queued);
        tasks = taskDao.getOrphanTasks(100, 19, TimeUnit.MINUTES);
        assertEquals(1, tasks.size());

        updateState(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20), TaskState.Running);
        tasks = taskDao.getOrphanTasks(100, 19, TimeUnit.MINUTES);
        assertEquals(1, tasks.size());

        updateState(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20), TaskState.Failure);
        tasks = taskDao.getOrphanTasks(100, 10, TimeUnit.MINUTES);
        assertEquals(0, tasks.size());

        updateState(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20), TaskState.Queued);
        tasks = taskDao.getOrphanTasks(100, 21, TimeUnit.MINUTES);
        assertEquals(0, tasks.size());
    }

    @Test
    public void setExitStatus() {
        assertTrue(taskDao.setExitStatus(task, 100));
        assertEquals(100, (int) jdbc.queryForObject("SELECT int_exit_status FROM task", Integer.class));
    }

    @Test
    public void setHost() {
        assertTrue(taskDao.setHost(task, "abc123"));
        assertEquals("abc123", jdbc.queryForObject("SELECT str_host FROM task", String.class));
    }

    @Test
    public void getTasks() {
        PagedList<Task> tasks = taskDao.getAll(job.getJobId(), Pager.first());
        assertEquals(1, tasks.size());
    }

    @Test
    public void incrementTaskStats() {
        taskDao.incrementStats(task.getTaskId(), 1, 2, 3);
        task = taskDao.get(task.getTaskId());
        assertEquals(1, task.getStats().getFrameSuccessCount());
        assertEquals(2, task.getStats().getFrameErrorCount());
        assertEquals(3, task.getStats().getFrameWarningCount());
    }

    private void updateState(final long time, TaskState state) {
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement("UPDATE task SET time_ping=?, time_state_change=?, int_state=? WHERE pk_task=?");
            ps.setLong(1, time);
            ps.setLong(2, time);
            ps.setInt(3, state.ordinal());
            ps.setInt(4, task.getTaskId());
            return ps;
        });
    }

    @Test
    public void testGetWaiting() {
        List<ExecuteTaskStart> waiting = taskDao.getWaiting(5);
        assertEquals(1, waiting.size());

        ExecuteTaskStart task = waiting.get(0);
        assertNotNull(task.getLogPath());
        assertNotNull(task.getJobId());
        assertNotNull(task.getArgs());
        assertNotNull(task.getEnv());
        assertNotNull(task.getScript());
    }

    @Test
    public void testGetLogPath() {
        Path lp = taskDao.getLogFilePath(task.getTaskId());
        assertTrue(lp.toString().startsWith(
                properties.getPath("zorroa.cluster.path.shared") + "/logs"));
    }

    @Test
    public void testGetScript() {
        String script = taskDao.getScript(task.getTaskId());
        // Just ensure the string parses to a ZpsScript
        ZpsScript zps = Json.deserialize(script, ZpsScript.class);
    }

    @Test
    public void testUpdatePingTime() {
        long timeA = jdbc.queryForObject(
                "SELECT time_ping FROM task WHERE pk_task=?", Long.class, task.getTaskId());

        taskDao.updatePingTime(ImmutableList.of(task.getTaskId()));

        long timeB = jdbc.queryForObject(
                "SELECT time_ping FROM task WHERE pk_task=?", Long.class, task.getTaskId());

        assertTrue(timeB > timeA);
    }
}
