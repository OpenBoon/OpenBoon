package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.zorroa.archivist.TestIngestor;
import com.zorroa.sdk.domain.IngestPipeline;
import com.zorroa.sdk.domain.IngestPipelineBuilder;
import com.zorroa.sdk.domain.IngestPipelineUpdateBuilder;
import com.zorroa.sdk.processor.ProcessorFactory;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/12/15.
 */
public class IngestPipelineControllerTests extends MockMvcTest {

    IngestPipeline pipeline;

    @Before
    public void init() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.setDescription("a test pipeline");
        builder.setProcessors(Lists.newArrayList(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ImageIngestor")));
        pipeline = ingestService.createIngestPipeline(builder);
    }

    @Test
    public void testDelete() throws Exception {

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(delete("/api/v1/pipelines/" + pipeline.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> status = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        assertTrue((Boolean)status.get("status"));
    }

    @Test
    public void update() throws Exception {

        IngestPipelineUpdateBuilder builder = new IngestPipelineUpdateBuilder();
        builder.setName("foo");
        builder.setDescription("Foo and Bar");
        builder.setProcessors(Lists.newArrayList(new ProcessorFactory<>(TestIngestor.class)));

        String v = Json.Mapper.writeValueAsString(builder);
        IngestPipelineUpdateBuilder b = Json.Mapper.readValue(v, IngestPipelineUpdateBuilder.class);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/pipelines/" + pipeline.getId())
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        IngestPipeline updated = Json.Mapper.readValue(result.getResponse().getContentAsString(), IngestPipeline.class);
        assertEquals(builder.getName(), updated.getName());
        assertEquals(builder.getDescription(), updated.getDescription());
        assertEquals(builder.getProcessors(), updated.getProcessors());
    }
}
