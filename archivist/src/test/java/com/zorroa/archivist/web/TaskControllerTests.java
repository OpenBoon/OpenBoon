package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 8/25/16.
 */
public class TaskControllerTests extends MockMvcTest {

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
        JobSpec spec = new JobSpec();
        spec.setName("bar");
        spec.setType(PipelineType.Export);
        job = jobService.launch(spec);

        tspec = new TaskSpec();
        tspec.setName("start");
        tspec.setScript(new ZpsScript());
        tspec.setJobId(job.getJobId());
        task = jobService.createTask(tspec);
    }

    @Test
    public void testStreamLog() throws Exception {
        MockHttpSession session = admin();

        Path logFile = Paths.get(taskDao.getExecutableTask(task.getTaskId()).getLogPath());
        new FileWriter(logFile.toFile()).append("bilbo baggins").close();

        MvcResult result = mvc.perform(get("/api/v1/tasks/" + task.getTaskId() + "/_log")
                .session(session)
                .contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        String data = result.getResponse().getContentAsString();
        assertEquals("bilbo baggins", data);
    }
}
