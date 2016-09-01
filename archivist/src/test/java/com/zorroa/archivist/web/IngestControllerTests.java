package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.Schedule;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.NestedServletException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/9/16.
 */
public class IngestControllerTests extends MockMvcTest {
    @Autowired
    IngestDao ingestDao;

    Ingest ingest;
    IngestSpec spec;

    @Before
    public void init() {
        spec = new IngestSpec();
        spec.setPipeline(Lists.newArrayList());
        spec.setGenerators(Lists.newArrayList());
        spec.setFolderId(null);
        spec.setPipelineId(null);
        spec.setName("Test");
        spec.setAutomatic(true);
        spec.setRunNow(false);
        spec.setSchedule(new Schedule());
        ingest = ingestDao.create(spec);
    }

    @Test
    public void testCreate() throws Exception {
        spec.setName("Test2");
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/ingests")
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest p = deserialize(result, Ingest.class);
    }

    @Test(expected=NestedServletException.class)
    public void testCreateWithValidationFailureNumericName() throws Exception {
        spec.setName("12345");
        MockHttpSession session = admin();
        mvc.perform(post("/api/v1/ingests")
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(delete("/api/v1/ingests/" + ingest.getId())
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        StatusResult rs = deserialize(result, StatusResult.class);
        assertTrue(rs.success);
    }

    @Test
    public void testUpdate() throws Exception {
        IngestSpec spec2 = new IngestSpec();
        spec2.setName("Rocky IV");
        spec2.setGenerators(ImmutableList.of());

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/ingests/" + ingest.getId())
                .session(session)
                .content(Json.serialize(spec2))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        StatusResult rs = deserialize(result, StatusResult.class);
        assertTrue(rs.success);
    }

    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/ingests/" + ingest.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest data = deserialize(result, Ingest.class);
        assertEquals(ingest, data);
    }


    @Test
    public void testGetByName() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/ingests/" + ingest.getName())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest data = deserialize(result, Ingest.class);
        assertEquals(ingest, data);
    }
}
