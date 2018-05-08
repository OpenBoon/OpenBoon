package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobSpec;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.processor.PipelineType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/19/16.
 */
public class ImportControllerTests extends MockMvcTest {

    @Autowired
    JobService jobService;

    JobSpec spec;
    Job job;
    Pipeline p;

    @Before
    public void init() {
        spec = new JobSpec();
        spec.setName("foo-bar");
        spec.setType(PipelineType.Import);
        job = jobService.launch(spec);


        PipelineSpecV spec = new PipelineSpecV()
                .setDescription("foo")
                .setName("foo")
                .setProcessors(ImmutableList.of())
                .setStandard(true)
                .setType(PipelineType.Import);
        p = pipelineService.create(spec);
    }
/*
    @Test
    public void testUpload() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile("files", "faces.jpg", "image/jpeg",
                        new FileInputStream(resources.resolve("images/set01/faces.jpg").toFile()));

        UploadImportSpec spec = new UploadImportSpec();
        spec.setName("unit test import");
        spec.setFiles(ImmutableList.of(file));
        spec.setPipelineIds(Lists.newArrayList(p.getId()));

        MockHttpSession session = admin();

        MvcResult result = mvc.perform(MockMvcRequestBuilders.fileUpload("/api/v1/imports/_upload")
                .file(file)
                .param("name", spec.getName())
                .param("pipelineIds", spec.getPipelineIds().stream().map(s->s.toString()).collect(Collectors.toList())
                .session(session))
                .andExpect(status().is(200))
                .andReturn();

        Job job = deserialize(result, Job.class);
        assertEquals(spec.getName(), job.getName());
    }
*/
    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/imports/" + job.getJobId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Job job = deserialize(result, Job.class);
        assertEquals(job.getJobId(), job.getId());
    }

    @Test
    public void testCancel() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/jobs/" + job.getJobId() + "/_cancel")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        StatusResult rs = deserialize(result, StatusResult.class);
        assertTrue(rs.success);
    }

    @Test
    public void testRestart() throws Exception {
        MockHttpSession session = admin();

        MvcResult cancel = mvc.perform(put("/api/v1/jobs/" + job.getJobId() + "/_cancel")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        StatusResult rs = deserialize(cancel, StatusResult.class);
        assertTrue(rs.success);

        MvcResult restart = mvc.perform(put("/api/v1/jobs/" + job.getJobId() + "/_restart")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        rs = deserialize(restart, StatusResult.class);
        assertTrue(rs.success);
    }
}
