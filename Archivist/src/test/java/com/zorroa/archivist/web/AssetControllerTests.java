package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.IngestService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
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
    IngestExecutorService ingestExecutorService;

    @Test
    public void testSearch() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
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
        ingestExecutorService.executeIngest(ingest);
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
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_aggregations")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"query\": { \"match_all\": {}}, \"aggregations\" : { \"Keywords\" : { \"terms\" : { \"field\" : \"keywords\" }}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> aggs = (Map<String, Object>)json.get("aggregations");
        Map<String, Object> keywords = (Map<String, Object>) aggs.get("Keywords");
        assertEquals(10, ((ArrayList<Map<String, Object>>) keywords.get("buckets")).size());
    }

    @Test
    public void testSuggest() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestExecutorService.executeIngest(ingest);
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
        ingestExecutorService.executeIngest(ingest);
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
            assertEquals(asset.getId(), json.get("_id"));
        }

    }

    @Test
    public void testFolderAssign() throws Exception {


        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();

        // Assign two collection names to each asset
        for (Asset asset : assets) {

            MvcResult result = mvc.perform(post("/api/v1/assets/" + asset.getId() + "/_folders")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("[\"foo\", \"bar\"]"))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(false, json.get("created"));
        }

        // Get each asset and verify that it has the assigned folders
        for (Asset asset: assets) {
            MvcResult result = mvc.perform(get("/api/v1/assets/" + asset.getId())
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(asset.getId(), json.get("_id"));
            Map<String, Object> source = (Map<String, Object>) json.get("_source");
            ArrayList<String> folders = (ArrayList<String>) source.get("folders");
            assertEquals(folders.size(), 2);
            assertEquals(folders.get(0), "foo");
            assertEquals(folders.get(1), "bar");
        }
    }

//    @Test
//    public void testFilteredSearch() throw Exception {
//
//    }

    @Test
    public void testFolderSearchBasic() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("standard")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();

        // Create two folders
        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"foo\", \"userId\": 1 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Folder foo = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {
                });

        result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"bar\", \"userId\": 1 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Folder bar = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        // Assign two collection names to each asset
        for (int i = 0; i < assets.size(); ++i) {
            Asset asset = assets.get(i);
            String folderId = (i % 2) == 0 ? foo.getId() : bar.getId();
            result = mvc.perform(post("/api/v1/assets/" + asset.getId() + "/_folders")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("[\"" + folderId + "\"]"))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(false, json.get("created"));
        }

        String folderJSON = new String(Json.serialize(foo), StandardCharsets.UTF_8);
        String query = "{ \"query\": { \"filtered\" : { \"query\" : { \"match_all\": {}}, \"folder\" : " + folderJSON + " }}}";
        result = mvc.perform(post("/api/v1/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(query.getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        assertEquals(1, (int) hits.get("total"));
    }

//    @Test
//    public void testFolderSearchFilter() throws Exception {
//    }
//
//    @Test
//    public void testSearchFolders() throws Exception {
//
//    }
//
//    @Test
//    public void testFilteredSearchFolders() throws Exception {
//
//    }
//
//    @Test
//    public void testFilteredSearchAndFilterFolders() throws Exception {
//
//    }
}
