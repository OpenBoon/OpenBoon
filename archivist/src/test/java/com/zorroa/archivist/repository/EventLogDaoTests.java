package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.common.cluster.thrift.StackElementT;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 8/31/16.
 */
public class EventLogDaoTests extends AbstractTest {

    @Autowired
    EventLogService logService;

    @Before
    public void init() {
        cleanElastic();
    }

    @Test
    public void testUserLogEntry() {
        refreshIndex();
        int count =  logService.getAll("user", new EventLogSearch()).size();
        logService.log(new UserLogSpec().setAction("test").setMessage("A test message"));
        refreshIndex();
        assertEquals(count+1,
                logService.getAll("user",  new EventLogSearch()).size());
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
        error.setOriginService("test");
        error.setPhase("process");
        error.setSkipped(true);
        error.setMessage("There was a failure");

        error.setStack(Lists.newArrayList(new StackElementT()
                .setClassName("com.zorroa.core.plugins.Processor")
                .setMethod("foo")
                .setLineNumber(10)
                .setFile("Processor.class")));

        logService.log(task, ImmutableList.of(error));
        refreshIndex();

        assertEquals(1,
                logService.getAll("job", new EventLogSearch()).size());
    }

}
