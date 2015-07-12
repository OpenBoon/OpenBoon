package com.zorroa.archivist.web;

import com.google.common.collect.Lists;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.processors.ChecksumProcessor;
import com.zorroa.archivist.processors.ProxyProcessor;
import com.zorroa.archivist.service.IngestService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/12/15.
 */
public class IngestPipelineControllerTests extends MockMvcTest {

    @Autowired
    IngestService ingestService;

    IngestPipeline pipeline;

    @Before
    public void init() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.setDescription("a test pipeline");
        builder.setProcessors(Lists.newArrayList(new IngestProcessorFactory(ChecksumProcessor.class)));
        pipeline = ingestService.createIngestPipeline(builder);
    }

    @Test
    public void update() throws Exception {

        IngestPipelineUpdateBuilder builder = new IngestPipelineUpdateBuilder();
        builder.setName("foo");
        builder.setDescription("Foo and Bar");
        builder.setProcessors(Lists.newArrayList(new IngestProcessorFactory(ProxyProcessor.class)));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/pipelines/" + pipeline.getId())
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        logger.info(result.getResponse().getContentAsString());

        IngestPipeline updated = Json.Mapper.readValue(result.getResponse().getContentAsString(), IngestPipeline.class);
        assertEquals(builder.getName(), updated.getName());
        assertEquals(builder.getDescription(), updated.getDescription());
        assertEquals(builder.getProcessors(), updated.getProcessors());
    }


}
