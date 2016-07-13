package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    public void getTask() {

    }

    @Test
    public void setExitStatus() {
        taskDao.setExitStatus(script, 1);
    }

    @Test
    public void setHost() {
        taskDao.setHost(script, "abc123");
    }
}
