package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.SearchService;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
    IngestExecutorService ingestExecutorService;

    @Test
    public void testSearchAll() throws Exception {

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
    public void testSearch() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"query\":{\"query_string\":{\"query\":\"beer\"}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertTrue(count == 1);
    }

    @Test
    public void testDateScriptSearch() throws Exception {
        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"query\":{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"script\":{\"script\":\"archivistDate\",\"lang\":\"native\",\"params\":{\"field\":\"source.date\",\"interval\":\"year\",\"terms\":[\"2014\"]}}}}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertTrue(count == 1);
    }

    @Test
    public void testSearchV2() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(new AssetSearchBuilder().setQuery("beer"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
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
    public void testCountV2() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_count")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(new AssetSearchBuilder().setQuery("beer"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> counts = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        int count = (int)counts.get("count");
        assertTrue(count == 1);
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
                .content("{ \"query\": { \"match_all\": {}}, \"aggregations\" : { \"Keywords\" : { \"terms\" : { \"field\" : \"keywords.all\" }}}}".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> aggs = (Map<String, Object>)json.get("aggregations");
        Map<String, Object> keywords = (Map<String, Object>) aggs.get("Keywords");
        assertEquals(10, ((ArrayList<Map<String, Object>>) keywords.get("buckets")).size());
    }

    @Test
    public void testAggregationV2() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_aggregate")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\" : \"Keywords\", \"field\" : \"keywords.all.raw\" }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> aggs = (Map<String, Object>)json.get("aggregations");
        Map<String, Object> keywords = (Map<String, Object>) aggs.get("Keywords");
        assertEquals(4, ((ArrayList<Map<String, Object>>) keywords.get("buckets")).size());
    }

    @Test
    public void testAggregationScriptV2() throws Exception {
        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_aggregate")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\" : \"Years\", \"script\" : \"archivistDate\", \"scriptParams\" : { \"field\" : \"source.date\", \"interval\" : \"year\" } }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> aggs = (Map<String, Object>)json.get("aggregations");
        Map<String, Object> years = (Map<String, Object>) aggs.get("Years");
        assertEquals(2, ((ArrayList<Map<String, Object>>) years.get("buckets")).size());
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
                .content("{ \"keyword-suggestions\": { \"text\": \"re\", \"completion\": { \"field\":\"keywords.suggest\"}}}".getBytes()))
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
    public void testSuggestV2() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_suggest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"text\": \"re\" }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});

        Map<String, Object> suggestions = (Map<String, Object>) ((ArrayList<Object>)json.get("completions")).get(0);
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
    public void testUpdate() throws Exception {

        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("canyon")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        List<Asset> assets = assetDao.getAll();
        Asset asset = assets.get(0);
        MvcResult result = mvc.perform(get("/api/v1/assets/" + asset.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals(asset.getId(), json.get("_id"));

        result = mvc.perform(put("/api/v1/assets/" + asset.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"source\" : { \"Xmp\" : { \"Rating\" : 3 } } }"))
                .andExpect(status().isOk())
                .andReturn();
        Asset xmp = assetDao.get(asset.getId());
        assertEquals(new Integer(3), xmp.getValue("Xmp.Rating"));
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

    @Test
    public void testFilteredSearch() throws Exception {
        MockHttpSession session = admin();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"filter\" : { \"fieldTerms\" : [ { \"field\" : \"File.FileName.raw\", \"terms\" : [ \"beer_kettle_01.jpg\" ] } ] } }"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testFolderSearchFilter() throws Exception {
        MockHttpSession session = user();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder().setName("TestSearchFolder"))))
                .andExpect(status().isOk())
                .andReturn();

        Folder folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        List<Asset> assets = assetDao.getAll();

        Asset asset = assets.get(0);
        result = mvc.perform(post("/api/v1/assets/" + asset.getId() + "/_folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("[\"" + folder.getId() + "\"]"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals(false, json.get("created"));

        result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"filter\" : { \"folderIds\" : [ \"" + folder.getId() + "\" ] } }"))
                .andExpect(status().isOk())
                .andReturn();

        json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testFilterExists() throws Exception {
        MockHttpSession session = user();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"filter\" : { \"existFields\" : [ \"Exif.CustomRendered\" ] } }"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testFilterRange() throws Exception {
        MockHttpSession session = user();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"filter\" : { \"fieldRanges\" : [ { \"field\" : \"source.date\", \"min\" : \"2014-01-01\", \"max\" : \"2015-01-01\" } ] } }"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testFilterScript() throws Exception {
        MockHttpSession session = user();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"filter\" : { \"scripts\" : [ { \"name\" : \"archivistDate\", \"params\" : { \"field\" : \"source.date\", \"interval\" : \"year\", \"terms\" : [\"2014\"] } } ] } }"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }
}
