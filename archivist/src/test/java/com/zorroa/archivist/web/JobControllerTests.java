package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.JobService;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 10/4/16.
 */
public class JobControllerTests extends MockMvcTest {

    @Autowired
    JobService jobService;

    JobSpec spec;
    Job job;
    TaskSpec tspec;
    Task task;

    @Before
    public void init() {
        JobSpec spec = new JobSpec();
        spec.setName("foo");
        spec.setType(PipelineType.Export);
        job = jobService.launch(spec);

        tspec = new TaskSpec();
        tspec.setName("task1");
        tspec.setScript(new ZpsScript());
        tspec.setJobId(job.getJobId());
        task = jobService.createTask(tspec);

        job = jobService.get(job.getId());
    }

    @Test
    public void testLaunch() throws Exception {
        MockHttpSession session = admin();

        ZpsScript script = new ZpsScript();
        script.setGenerate(ImmutableList.of(
                new ProcessorRef("com.zorroa.core.generator.AssetSearchGenerator")
                        .setArg("search", new AssetSearch("test"))));

        script.setExecute(ImmutableList.of(
                new ProcessorRef("com.zorroa.core.processor.GroupProcessor")));

        JobSpecV spec = new JobSpecV();
        spec.setScript(script);
        spec.setName("Test job");
        spec.setArgs(ImmutableMap.of("arg1", "value1"));
        spec.setEnv(ImmutableMap.of("env1", "value1"));
        spec.setType(PipelineType.Import);

        MvcResult result = mvc.perform(post("/api/v1/jobs")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(status().isOk())
                .andReturn();

        Job job1 = deserialize(result, Job.class);
        Job job2 = jobService.get(job1.getId());
        assertEquals(job1, job2);
        assertEquals(1, job2.getCounts().getTasksTotal());

        PagedList<Task> tasks = jobService.getAllTasks(job2.getJobId(), Pager.first());
        assertEquals(1, tasks.size());
    }

    @Test(expected=org.springframework.web.util.NestedServletException.class)
    public void testLaunchValidationFailure() throws Exception {
        MockHttpSession session = admin();

        JobSpecV spec = new JobSpecV();
        mvc.perform(post("/api/v1/jobs")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void getTasksByJob() throws Exception {
        MockHttpSession session = admin();

        MvcResult result = mvc.perform(get("/api/v1/jobs/" + job.getId() + "/tasks")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        PagedList<Task> tasks = deserialize(result, new TypeReference<PagedList<Task>>() {});
        assertEquals(1, tasks.size());
    }

    @Test
    public void getTasks() throws Exception {
        MockHttpSession session = admin();

        TaskFilter f = new TaskFilter();
        f.setStates(ImmutableSet.of(TaskState.Waiting));
        f.setSort(ImmutableMap.of("taskId", "desc"));

        MvcResult result = mvc.perform(post("/api/v2/jobs/" + job.getId() + "/tasks")
                .session(session)
                .content(Json.serialize(f))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        PagedList<Task> tasks = deserialize(result, new TypeReference<PagedList<Task>>() {});
        assertEquals(1, tasks.size());
    }

}
