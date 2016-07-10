package com.zorroa.archivist.web;

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpec;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.NestedServletException;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/12/15.
 */
public class PipelineControllerTests extends MockMvcTest {

    @Autowired
    PipelineDao pipelineDao;

    Pipeline pl;
    PipelineSpec spec;

    @Before
    public void init() {
        spec = new PipelineSpec();
        spec.setType(PipelineType.Import);
        spec.setProcessors(Lists.newArrayList());
        spec.setName("Zorroa Test");
    }

    @Test
    public void testCreate() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/pipelines")
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Pipeline p = deserialize(result, Pipeline.class);
        assertEquals(spec.getType(), p.getType());
        assertEquals(spec.getProcessors(), p.getProcessors());
        assertEquals(spec.getName(), p.getName());
    }

    @Test(expected=NestedServletException.class)
    public void testCreateWithValidationFailureNumericName() throws Exception {
        spec.setName("12345");
        MockHttpSession session = admin();
        mvc.perform(post("/api/v1/pipelines")
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testDelete() throws Exception {
        pl = pipelineService.create(spec);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(delete("/api/v1/pipelines/" + pl.getId())
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = deserialize(result, Map.class);
        assertEquals(true, data.get("result"));
    }

    @Test
    public void testUpdate() throws Exception {
        pl = pipelineService.create(spec);

        PipelineSpec spec2 = new PipelineSpec();
        spec2.setType(PipelineType.Update);
        spec2.setProcessors(Lists.newArrayList());
        spec2.setName("Rocky IV");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/pipelines/" + pl.getId())
                .session(session)
                .content(Json.serialize(spec2))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = deserialize(result, Map.class);
        assertEquals(true, data.get("result"));
    }

    @Test
    public void testGet() throws Exception {
        pl = pipelineService.create(spec);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/pipelines/" + pl.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Pipeline data = deserialize(result, Pipeline.class);
        assertEquals(pl, data);
    }


    @Test
    public void testGetByName() throws Exception {
        pl = pipelineService.create(spec);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/pipelines/" + pl.getName())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Pipeline data = deserialize(result, Pipeline.class);
        assertEquals(pl, data);
    }
}
