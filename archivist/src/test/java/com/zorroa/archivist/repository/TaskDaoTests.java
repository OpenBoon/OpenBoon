package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

    ZpsScript script;

    @Before
    public void init() {
        script = new ZpsScript();
        script.setName("foo-bar");
        jobService.launch(script, PipelineType.Import);
    }

    @Test
    public void testCreate() {
        assertNotNull(script.getTaskId());
    }

    @Test
    public void testCreateChild() {
        assertNotNull(script.getTaskId());
        taskDao.create(script);
    }

    @Test
    public void testSetState() {
        assertTrue(taskDao.setState(script, TaskState.Queued, TaskState.Waiting));
        assertFalse(taskDao.setState(script, TaskState.Queued, TaskState.Waiting));
    }

    @Test
    public void getWaiting() {
        assertEquals(1, taskDao.getWaiting(5).size());
    }

    @Test
    public void getOrphanTasks() {
        List<ZpsTask> tasks = taskDao.getOrphanTasks(100, 20, TimeUnit.MINUTES);
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
        assertTrue(taskDao.setExitStatus(script, 100));
        assertEquals(100, (int) jdbc.queryForObject("SELECT int_exit_status FROM task", Integer.class));
    }

    @Test
    public void setHost() {
        assertTrue(taskDao.setHost(script, "abc123"));
        assertEquals("abc123", jdbc.queryForObject("SELECT str_host FROM task", String.class));
    }

    private void updateState(final long time, TaskState state) {
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement("UPDATE task SET time_state_change=?, int_state=? WHERE pk_task=?");
            ps.setLong(1, time);
            ps.setInt(2, state.ordinal());
            ps.setInt(3, script.getTaskId());
            return ps;
        });
    }
}
