package com.zorroa.archivist.web;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.service.IngestSchedulerService;
import com.zorroa.archivist.service.IngestService;

public class IngestControllerTests extends MockMvcTest {

    @Autowired
    AssetController assetController;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    Ingest ingest;

    @Before
    public void init() {
        ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
    }

    @Test
    public void testGetAll() throws Exception {
        MockHttpSession session = admin();

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeNextIngest();
        refreshIndex(1000);

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
    public void testGetPending() throws Exception {
        MockHttpSession session = admin();

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeNextIngest();
        refreshIndex(1000);

        MvcResult result = mvc.perform(get("/api/v1/ingests?state=pending")
                .session(session)
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
        builder.setPath(getStaticImagePath());
        builder.setFileTypes(Sets.newHashSet("jpg"));

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
    public void testIngest() throws Exception {

        IngestBuilder builder = new IngestBuilder();
        builder.setPath(getStaticImagePath());
        builder.setFileTypes(Sets.newHashSet("jpg"));

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

        result = mvc.perform(post("/api/v1/ingests/" + ingest.getId() + "/_ingest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Ingest finishedIngest = Json.Mapper.readValue(result.getResponse().getContentAsString(), Ingest.class);
        assertEquals(ingest.getId(), finishedIngest.getId());
    }
}
