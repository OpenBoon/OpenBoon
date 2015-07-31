package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.service.IngestSchedulerService;
import com.zorroa.archivist.service.IngestService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"query\": { \"match_all\": {}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertTrue(count == 2);
    }

    @Test
    public void testCount() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_count")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"query\": { \"match_all\": {}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> counts = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        int count = (int)counts.get("count");
        assertTrue(count == 2);
    }

    @Test
    public void testAggregation() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_aggregations")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"query\": { \"match_all\": {}}, \"aggregations\" : { \"Keywords\" : { \"terms\" : { \"field\" : \"keywords\" }}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {
                });
        Map<String, Object> aggs = (Map<String, Object>)json.get("aggregations");
        Map<String, Object> keywords = (Map<String, Object>) aggs.get("Keywords");
        assertEquals(10, ((ArrayList<Map<String, Object>>) keywords.get("buckets")).size());
    }

    @Test
    public void testSuggest() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_suggest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"keyword-suggestions\": { \"text\": \"re\", \"completion\": { \"field\":\"keywords_suggest\"}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});

        Map<String, Object> suggestions = (Map<String, Object>) ((ArrayList<Object>)json.get("keyword-suggestions")).get(0);
        ArrayList<Object> options = (ArrayList<Object>) suggestions.get("options");
        Map<String, Object> suggestion = (Map<String, Object>) options.get(0);
        String text = (String)suggestion.get("text");
        assertTrue(text.equals("reflection"));
    }

    @Test
    public void testGet() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestSchedulerService.executeIngest(ingest);
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

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();

        // Assign two collection names to each asset
        for (Asset asset : assets) {

            MvcResult result = mvc.perform(post("/api/v1/assets/" + asset.getId() + "/_collections")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("[\"foo\", \"bar\"]"))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(false, (boolean) json.get("created"));
        }

        // Get each asset and verify that it has the assigned collections
        for (Asset asset: assets) {
            MvcResult result = mvc.perform(get("/api/v1/assets/" + asset.getId())
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(asset.getId(), (String) json.get("_id"));
            Map<String, Object> source = (Map<String, Object>) json.get("_source");
            ArrayList<String> collections = (ArrayList<String>) source.get("collections");
            assertEquals(collections.size(), 2);
            assertEquals(collections.get(0), "foo");
            assertEquals(collections.get(1), "bar");
        }

    }
}
