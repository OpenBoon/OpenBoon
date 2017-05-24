package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.sdk.domain.Pager;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 8/31/16.
 */
public class EventLogDaoTests extends AbstractTest {


    @Autowired
    EventLogService logService;

    @Test
    public void testUserLogEntry() {
        refreshIndex();
        int count =  logService.getAll("user", new EventLogSearch(), Pager.first()).size();
        logService.log(new UserLogSpec().setAction("test").setMessage("A test message"));
        refreshIndex();
        assertEquals(count+1,
                logService.getAll("user",  new EventLogSearch(), Pager.first()).size());
    }

    @Test
    public void testJobLogEntry() {
        Task task = new Task();
        task.setJobId(1);
        task.setTaskId(100);

        TaskErrorT error = new TaskErrorT();
        error.setPath("/foo/bar");
        error.setTimestamp(System.currentTimeMillis());
        error.setOriginPath("/shoe");
        error.setLineNumber(10);
        error.setClassName("com.zorroa.core.plugins.Processor");
        error.setFile("Processor.class");
        error.setOriginService("test");
        error.setPhase("process");
        error.setSkipped(true);
        error.setMessage("There was a failure");

        logService.log(task, ImmutableList.of(error));
        refreshIndex();

        assertEquals(1,
                logService.getAll("job", new EventLogSearch(), Pager.first()).size());
    }

}
