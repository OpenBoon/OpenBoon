package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobSpec;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.domain.UploadImportSpec;
import com.zorroa.archivist.service.JobService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.FileInputStream;

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

    @Before
    public void init() {
        spec = new JobSpec();
        spec.setName("foo-bar");
        spec.setType(PipelineType.Import);
        job = jobService.launch(spec);
    }

    @Test
    public void testUpload() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile("files", "zorroa-test-plugin.zip", "image/jpeg",
                        new FileInputStream(new File("../unittest/resources/images/set01/faces.jpg")));

        UploadImportSpec spec = new UploadImportSpec();
        spec.setName("unit test import");
        spec.setFiles(ImmutableList.of(file));
        spec.setPipelineId(pipelineService.get("Zorroa Example (Vision)").getId());

        MockHttpSession session = admin();

        MvcResult result = mvc.perform(MockMvcRequestBuilders.fileUpload("/api/v1/imports/_upload")
                .file(file)
                .param("name", spec.getName())
                .param("pipelineId", String.valueOf(spec.getPipelineId()))
                .session(session))
                .andExpect(status().is(200))
                .andReturn();

        Job job = deserialize(result, Job.class);
        assertEquals(spec.getName(), job.getName());
    }

    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/imports/" + job.getJobId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Job job = deserialize(result, Job.class);
        assertEquals((int) job.getJobId(), job.getId());
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
