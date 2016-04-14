package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.zorroa.archivist.TestSearchResult;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.PermissionSchema;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.common.repository.AssetDao;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AssetControllerTests extends MockMvcTest {

    @Autowired
    AssetController assetController;

    @Autowired
    AssetDao assetDao;


    @Test
    public void testGetFields() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("standard");

        MvcResult result = mvc.perform(get("/api/v1/assets/_fields")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Set<String>> fields = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Set<String>>>() {});
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.get("integer").size() > 0);
    }

    @Test
    public void testSearchV2() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("standard");

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(new AssetSearch("beer"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testCountV2() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("standard");

        MvcResult result = mvc.perform(post("/api/v2/assets/_count")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(new AssetSearch("beer"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> counts = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        int count = (int)counts.get("count");
        assertEquals(1, count);
    }

    @Test
    public void testAggregationV2() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("agg");

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
        assertEquals(3, ((ArrayList<Map<String, Object>>) keywords.get("buckets")).size());
    }

    @Test
    public void testAggregationScriptV2() throws Exception {
        MockHttpSession session = admin();
        addTestAssets("agg");

        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put("field", "source.date");
        scriptParams.put("interval", "year");
        AssetAggregateBuilder aab = new AssetAggregateBuilder().setName("Years").setScript(new AssetScript().setScript("archivistDate").setParams(scriptParams));
        MvcResult result = mvc.perform(post("/api/v2/assets/_aggregate")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(aab)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> aggs = (Map<String, Object>)json.get("aggregations");
        Map<String, Object> years = (Map<String, Object>) aggs.get("Years");
        assertEquals(1, ((ArrayList<Map<String, Object>>) years.get("buckets")).size());
    }

    @Test
    public void testSuggest() throws Exception {

        MockHttpSession session = admin();
        List<AssetBuilder> builders = getTestAssets("canyon");
        for (AssetBuilder builder: builders) {
            builder.addSuggestKeywords("source", "reflection");
        }
        addTestAssets(builders);

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
        List<AssetBuilder> builders = getTestAssets("canyon");
        for (AssetBuilder builder: builders) {
            builder.addSuggestKeywords("source", "reflection");
        }
        addTestAssets(builders);

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
        addTestAssets("standard");

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
    public void testUpdateRating() throws Exception {

        MockHttpSession session = admin();

        addTestAssets("canyon");

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

        AssetUpdateBuilder update = new AssetUpdateBuilder();
        update.setRating(3);

        mvc.perform(put("/api/v1/assets/" + asset.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(update)))
                .andExpect(status().isOk())
                .andReturn();
        Asset updated = assetDao.get(asset.getId());
        assertEquals(new Integer(3), updated.getAttr("user.rating"));
    }


    @Test
    public void testUpdatePermissions() throws Exception {

        MockHttpSession session = admin();

        addTestAssets("canyon");

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

        AssetUpdateBuilder update = new AssetUpdateBuilder();
        update.setPermissions(new PermissionSchema().setSearch(Sets.newHashSet(1)));

        mvc.perform(put("/api/v1/assets/" + asset.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(update)))
                .andExpect(status().isOk())
                .andReturn();
        Asset updated = assetDao.get(asset.getId());
        PermissionSchema updatedPermissions = updated.getAttr("permissions", PermissionSchema.class);
        assertEquals(Sets.newHashSet(1), updatedPermissions.getSearch());
    }

    @Test
    public void testFolderAssign() throws Exception {
        MockHttpSession session = admin();

        addTestAssets("canyon");
        List<Asset> assets = assetDao.getAll();

        Folder folder1 = folderService.create(new FolderBuilder("foo"));
        Folder folder2 = folderService.create(new FolderBuilder("bar"));
        mvc.perform(post("/api/v1/folders/" + folder1.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map(Asset::getId).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(post("/api/v1/folders/" + folder2.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map(Asset::getId).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn();

        assets = assetDao.getAll();
        for (Asset asset: assets) {
            Set<Integer> folderIds = asset.getAttr("folders", new TypeReference<Set<Integer>>() {});
            assertTrue(folderIds.contains(folder1.getId()));
            assertTrue(folderIds.contains(folder2.getId()));
        }
    }

    @Test
    public void testFilteredSearch() throws Exception {
        MockHttpSession session = admin();

        addTestAssets("standard");

        AssetSearch search = new AssetSearch(new AssetFilter().addToFieldTerms("source.filename.raw", "beer_kettle_01.jpg"));
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
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

        addTestAssets("standard");

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
        mvc.perform(post("/api/v1/folders/" + folder.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableList.of(asset.getId()))))
                .andExpect(status().isOk())
                .andReturn();

        AssetSearch search = new AssetSearch(new AssetFilter().addToFolderIds(folder.getId()));

        result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
                .andExpect(status().isOk())
                .andReturn();

        TestSearchResult searchResult = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), TestSearchResult.class);
        assertEquals(1, searchResult.getHits().getTotal());
    }

    @Test
    public void testFolderChildrenSearchFilter() throws Exception {
        MockHttpSession session = user();

        addTestAssets("standard");

        // Create two folders, a parent and its child
        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder().setName("ParentSearchFolder"))))
                .andExpect(status().isOk())
                .andReturn();

        Folder parent = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder().setName("ChildSearchFolder").setParentId(parent.getId()))))
                .andExpect(status().isOk())
                .andReturn();

        Folder child = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertNotEquals(child.getId(), parent.getId());

        // Put a single asset into each folder
        List<Asset> assets = assetDao.getAll();

        Asset asset = assets.get(0);
        mvc.perform(post("/api/v1/folders/" + parent.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableList.of(asset.getId()))))
                .andExpect(status().isOk())
                .andReturn();

        asset = assets.get(1);
        mvc.perform(post("/api/v1/folders/" + child.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableList.of(asset.getId()))))
                .andExpect(status().isOk())
                .andReturn();

        AssetSearch search = new AssetSearch(new AssetFilter().addToFolderIds(child.getId()));

        // Searching the child should return a single hit
        result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);

        search = new AssetSearch();
        // Searching without folders returns all hits, which is only two
        result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
                .andExpect(status().isOk())
                .andReturn();

        json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        hits = (Map<String, Object>) json.get("hits");
        logger.info(result.getResponse().getContentAsString());
        count = (int)hits.get("total");
        assertEquals(2, count);

        search = new AssetSearch(new AssetFilter().addToFolderIds(parent.getId()));

        // Searching the parent folder should return two hits as well
        result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
                .andExpect(status().isOk())
                .andReturn();

        json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        hits = (Map<String, Object>) json.get("hits");
        count = (int)hits.get("total");
        assertEquals(2, count);

        // Remove the child from the parent folder
        result = mvc.perform(put("/api/v1/folders/" + child.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder())))
                .andExpect(status().isOk())
                .andReturn();

        child = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertNotEquals(child.getParentId().intValue(), parent.getId());

        search = new AssetSearch(new AssetFilter().addToFolderIds(parent.getId()));

        // Searching the parent should now return a single hit
        result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
                .andExpect(status().isOk())
                .andReturn();

        json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        hits = (Map<String, Object>) json.get("hits");
        count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testEmptySearch() throws Exception {
        MockHttpSession session = user();

        addTestAssets("standard");

        AssetSearch search = new AssetSearch("");
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(search)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(2, count);     // Total count is 2, even though we only get 1 asset in array below
    }

    @Test
    public void testFromSize() throws Exception {
        MockHttpSession session = user();

        addTestAssets("standard");

        AssetSearch search = new AssetSearch().setFrom(1).setSize(1);
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(search)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(2, count);     // Total count is 2, even though we only get 1 asset in array below
        ArrayList<Object> assets = (ArrayList<Object>) hits.get("hits");
        assertEquals(1, assets.size());
    }

    @Test
    public void testFilterIngest() throws Exception {
        MockHttpSession session = user();

        Ingest ingest = addTestAssets("canyon");

        AssetSearch search = new AssetSearch(new AssetFilter().setIngestId(ingest.getId()));
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
    }

    @Test
    public void testFilterExists() throws Exception {
        MockHttpSession session = user();
        addTestAssets("canyon");

        AssetSearch search = new AssetSearch(new AssetFilter().addToExistFields("source.path"));

        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(search)))
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

        addTestAssets("standard");

        DateTime dateTime = new DateTime();
        int year = dateTime.getYear();

        AssetFieldRange range = new AssetFieldRange()
                .setField("source.date")
                .setMin(String.format("%d-01-01", year-1)).setMax(String.format("%d-01-01", year+1));
        ArrayList<AssetFieldRange> ranges = new ArrayList<>();
        ranges.add(range);

        AssetSearch search = new AssetSearch(new AssetFilter().setFieldRanges(ranges));
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(search)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(2, count);
    }

    @Test
    public void testFilterScript() throws Exception {
        MockHttpSession session = user();
        addTestAssets("standard");

        DateTime dateTime = new DateTime();
        int year = dateTime.getYear();

        Map<String, Object> scriptParams = new HashMap<>();
        scriptParams.put("field", "source.date");
        scriptParams.put("interval", "year");
        List<String> terms = new ArrayList<>();
        terms.add(String.valueOf(year));
        scriptParams.put("terms", terms);
        AssetScript script = new AssetScript().setScript("archivistDate").setParams(scriptParams);
        List<AssetScript> scripts = new ArrayList<>();
        scripts.add(script);
        AssetSearch asb = new AssetSearch(new AssetFilter().setScripts(scripts));
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(asb)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(2, count);
    }

    @Test
    public void testFilterAsset() throws Exception {
        MockHttpSession session = user();

        addTestAssets("standard");

        ArrayList<String> assetIds = new ArrayList<>();
        List<Asset> assets = assetDao.getAll();
        Asset asset = assets.get(0);
        assetIds.add(asset.getId());
        AssetSearch asb = new AssetSearch(new AssetFilter().setAssetIds(assetIds));
        MvcResult result = mvc.perform(post("/api/v2/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(asb)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        Map<String, Object> hits = (Map<String, Object>) json.get("hits");
        int count = (int)hits.get("total");
        assertEquals(1, count);
        ArrayList<Map<String, Object>> hitAssets = (ArrayList<Map<String, Object>>)hits.get("hits");
        Map<String, Object> doc = (Map<String, Object>)hitAssets.get(0);
        assertEquals(asset.getId(), doc.get("_id"));
    }
}
