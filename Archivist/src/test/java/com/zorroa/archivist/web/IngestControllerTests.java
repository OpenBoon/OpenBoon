package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.IngestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IngestControllerTests extends MockMvcTest {

    @Autowired
    AssetController assetController;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    IngestExecutorService ingestExecutorService;

    Ingest ingest;

    @Before
    public void init() {
        ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
    }

    @Test
    public void testGetAll() throws Exception {
        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        MvcResult result = mvc.perform(get("/api/v1/ingests")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Ingest> ingests = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Ingest>>() {});
        assertEquals(2, ingests.size());
    }

    @Test
    public void testSearchByState() throws Exception {
        MockHttpSession session = admin();

        IngestFilter filter = new IngestFilter();
        filter.setStates(EnumSet.of(IngestState.Idle));

        MvcResult result = mvc.perform(post("/api/v1/ingests/_search")
                .session(session)
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Ingest> ingests = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Ingest>>() {});
        assertEquals(1, ingests.size());
    }

    @Test
    public void testSearchByPipeline() throws Exception {
        MockHttpSession session = admin();

        IngestFilter filter = new IngestFilter();
        filter.setPipelines(Lists.newArrayList("standard"));

        MvcResult result = mvc.perform(post("/api/v1/ingests/_search")
                .session(session)
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Ingest> ingests = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Ingest>>() {});
        assertEquals(1, ingests.size());
    }

    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/ingests/" + ingest.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest ingest = Json.Mapper.readValue(result.getResponse().getContentAsString(), Ingest.class);
        assertEquals(this.ingest.getId(), ingest.getId());
    }

    @Test
    public void testCreate() throws Exception {

        IngestBuilder builder = new IngestBuilder();
        builder.addToPaths(getStaticImagePath());

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/ingests")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest ingest = Json.Mapper.readValue(result.getResponse().getContentAsString(), Ingest.class);
        assertEquals(ingest.getId(), ingest.getId());
    }

    @Test
    public void testUpdate() throws Exception {

        IngestUpdateBuilder builder = new IngestUpdateBuilder();
        builder.addToPaths("/vol/data");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/ingests/" + ingest.getId())
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest updatedIngest = Json.Mapper.readValue(result.getResponse().getContentAsString(), Ingest.class);
        assertEquals(ingest.getId(), updatedIngest.getId());

        assertEquals(builder.getPaths(), updatedIngest.getPaths());
    }

    @Test
    public void testDelete() throws Exception {

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(delete("/api/v1/ingests/" + ingest.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> status = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        assertTrue((Boolean)status.get("status"));
    }


    @Test
    public void testExecute() throws Exception {

        IngestBuilder builder = new IngestBuilder();
        builder.addToPaths(getStaticImagePath());

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/ingests/")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest ingest = Json.Mapper.readValue(result.getResponse().getContentAsString(), Ingest.class);
        assertEquals(ingest.getId(), ingest.getId());

        refreshIndex();

        result = mvc.perform(post("/api/v1/ingests/" + ingest.getId() + "/_execute")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest finishedIngest = Json.Mapper.readValue(result.getResponse().getContentAsString(), Ingest.class);
        assertEquals(ingest.getId(), finishedIngest.getId());
    }
}
