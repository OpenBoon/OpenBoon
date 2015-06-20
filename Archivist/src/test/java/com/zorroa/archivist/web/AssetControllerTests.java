package com.zorroa.archivist.web;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.service.IngestSchedulerService;
import com.zorroa.archivist.service.IngestService;

public class AssetControllerTests extends MockMvcTest {

    @Autowired
    AssetController assetController;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @Test
    public void testSearch() throws Exception {

        MockHttpSession session = admin();

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeNextIngest();
        refreshIndex(1000);

        MvcResult result = mvc.perform(get("/api/v1/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"query\": { \"match_all\": {}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        assertEquals(2, (int) hits.get("total"));
    }

    @Test
    public void testCount() throws Exception {

        MockHttpSession session = admin();

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeNextIngest();
        refreshIndex(1000);

        MvcResult result = mvc.perform(get("/api/v1/assets/_count")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"query\": { \"match_all\": {}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> counts = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});

        assertEquals(2, (int) counts.get("count"));
    }

    @Test
    public void testGet() throws Exception {

        MockHttpSession session = admin();

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();

        for (Asset asset: assets) {

            MvcResult result = mvc.perform(get("/api/v1/assets/" + asset.getId())
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(asset.getId(), (String) json.get("_id"));
        }

    }

    @Test
    public void testCollections() throws Exception {

        MockHttpSession session = admin();

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();

        for (Asset asset : assets) {

            MvcResult result = mvc.perform(post("/api/v1/assets/" + asset.getId() + "/_collections")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("[\"foo\", \"bar\"]"))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {
                    });
            assertEquals(false, (boolean) json.get("created"));
            assertEquals(Integer.valueOf(2), (Integer) json.get("version"));
        }
    }
}
