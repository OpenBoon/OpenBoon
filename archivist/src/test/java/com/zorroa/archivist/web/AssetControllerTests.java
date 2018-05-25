package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.web.api.AssetController;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.domain.Proxy;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AssetControllerTests extends MockMvcTest {

    @Autowired
    AssetDao assetDao;

    @Autowired
    AssetController assetController;

    @Autowired
    ObjectFileSystem ofs;

    @Before
    public void init() {
        fieldService.invalidateFields();
    }

    @Test
    public void testGetFields() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");

        MvcResult result = mvc.perform(get("/api/v1/assets/_fields")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Set<String>> fields = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Set<String>>>() {});
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.containsKey("integer"));
    }

    @Test
    public void testHideAndUnhideField() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");

        MvcResult result = mvc.perform(put("/api/v1/assets/_fields/hide")
                .session(session)
                .content(Json.serializeToString(ImmutableMap.of("field", "source.")))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> status = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        assertTrue((Boolean) status.get("success"));

        fieldService.invalidateFields();
        Map<String,Set<String>> fields = fieldService.getFields("asset");
        for (String field: fields.get("string")) {
            assertFalse(field.startsWith("source"));
        }

        mvc.perform(delete("/api/v1/assets/_fields/hide")
                .session(session)
                .content(Json.serializeToString(ImmutableMap.of("field", "source.")))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        status = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
        assertTrue((Boolean) status.get("success"));

        Map<String,Set<String>> stringFields = fieldService.getFields("asset");
        assertNotEquals(fields, stringFields);
    }

    @Test
    public void testSearchV3() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");

        MvcResult result = mvc.perform(post("/api/v3/assets/_search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(new AssetSearch("O'Malley"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {});
    }

    @Test
    public void testCountV2() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");

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
    public void testSuggestV3() throws Exception {
        MockHttpSession session = admin();
        List<Source> sources = getTestAssets("set04/canyon");
        for (Source source: sources) {
            source.addToKeywords("media", "reflection");
        }
        addTestAssets(sources);

        refreshIndex();

        MvcResult result = mvc.perform(post("/api/v3/assets/_suggest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"text\": \"re\" }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<String> keywords = Json.Mapper.readValue(json, Json.LIST_OF_STRINGS);

        assertTrue("The list of keywords, '" + json + "' does not contain 'reflection'",
                keywords.contains("reflection"));
    }

    @Test
    public void testSuggestV3MultipleFields() throws Exception {
        MockHttpSession session = admin();
        List<Source> sources = getTestAssets("set04/canyon");
        for (Source source: sources) {
            source.addToKeywords("media","reflection");
            source.setAttr("thing.suggest", "resume");
        }
        addTestAssets(sources);
        refreshIndex();

        MvcResult result = mvc.perform(post("/api/v3/assets/_suggest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"text\": \"re\" }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<String> keywords = Json.Mapper.readValue(json,  Json.LIST_OF_STRINGS);

        assertTrue("The list of keywords, '" + json + "' does not contain 'reflection'",
                keywords.contains("reflection"));
        assertTrue("The list of keywords, '" + json + "' does not contain 'resume'",
                keywords.contains("reflection"));
    }

    @Test
    public void testDelete() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");
        refreshIndex();

        PagedList<Document> assets = assetDao.getAll(Pager.first());
        for (Document asset: assets) {
            MvcResult result = mvc.perform(delete("/api/v1/assets/" + asset.getId())
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(true, json.get("success"));
            assertEquals("delete", json.get("op"));
        }
    }

    @Test
    public void testGetV2() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");
        refreshIndex();

        PagedList<Document> assets = assetDao.getAll(Pager.first());
        for (Document asset: assets) {
            MvcResult result = mvc.perform(get("/api/v2/assets/" + asset.getId())
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(asset.getId(), json.get("id"));
        }
    }

    @Test
    public void testGetByPath() throws Exception {

        MockHttpSession session = admin();
        addTestAssets("set04/standard");
        refreshIndex();

        PagedList<Document> assets = assetDao.getAll(Pager.first());
        for (Document asset: assets) {
            String url = "/api/v1/assets/_path";
            MvcResult result = mvc.perform(get(url)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serializeToString(ImmutableMap.of("path", asset.getAttr("source.path")))))
                    .andExpect(status().isOk())
                    .andReturn();
            Map<String, Object> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                    new TypeReference<Map<String, Object>>() {});
            assertEquals(asset.getId(), json.get("id"));
        }
    }

    @Test
    public void testSetFolders() throws Exception {
        authenticate("admin");
        addTestAssets("set04/canyon");

        List<UUID> folders = Lists.newArrayList();
        for (int i=0; i<10; i++) {
            FolderSpec builder = new FolderSpec("Folder" + i);
            Folder folder = folderService.create(builder);
            folders.add(folder.getId());
        }

        List<Document> assets = indexService.getAll(Pager.first(1)).getList();
        assertEquals(1, assets.size());
        Document doc = assets.get(0);

        MockHttpSession session = admin();
        mvc.perform(put("/api/v1/assets/" + doc.getId() + "/_setFolders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("folders", folders))))
                .andExpect(status().isOk())
                .andReturn();

        refreshIndex();
        doc = indexService.get(doc.getId());
        assertEquals(10, doc.getAttr("zorroa.links.folder", List.class).size());

    }

    @Test
    public void testFolderAssign() throws Exception {
        MockHttpSession session = admin();

        addTestAssets("set04/canyon");
        PagedList<Document> assets = assetDao.getAll(Pager.first());

        Folder folder1 = folderService.create(new FolderSpec("foo"));
        Folder folder2 = folderService.create(new FolderSpec("bar"));
        mvc.perform(post("/api/v1/folders/" + folder1.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map(Document::getId).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(post("/api/v1/folders/" + folder2.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map(Document::getId).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn();

        refreshIndex();
        assets = assetDao.getAll(Pager.first());
        for (Document asset: assets) {
            getLogger().info("{}", asset.getDocument());
            List<String> links = asset.getAttr("zorroa.links.folder", new TypeReference<List<String>>() {});
            assertEquals(2, links.size());
            assertTrue(
                    links.get(0).equals(folder1.getId().toString()) ||
                    links.get(1).equals(folder1.getId().toString()));
        }
    }


    @Test
    public void testStream() throws Exception {
        MockHttpSession session = admin();
        addTestAssets("set04/standard");
        refreshIndex();

        PagedList<Document> assets = indexService.getAll(Pager.first());

        String url = String.format("/api/v1/assets/%s/_stream", assets.get(0).getId());
        mvc.perform(get(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

    }

    @Test
    public void testStream404() throws Exception {
        MockHttpSession session = admin();
        addTestAssets("set04/standard");
        refreshIndex();

        PagedList<Document> assets = indexService.getAll(Pager.first());

        String url = String.format("/api/v1/assets/%s/_stream?ext=foo", assets.get(0).getId());
        MvcResult result = mvc.perform(get(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void testGetPreferredFormat() throws Exception {
        MockHttpSession session = admin();
        addTestAssets("/video");
        refreshIndex();

        PagedList<Document> assets = indexService.getAll(Pager.first());
        AssetController.StreamFile file = assetController.getPreferredFormat(assets.get(0), "m4v",
                false, false);

        assertNotNull(file);
        assertEquals("video/x-m4v", file.getMimeType());

        file = assetController.getPreferredFormat(assets.get(0), "ogv",
                false, false);
        assertNull(file);
    }

    @Test
    public void testGetPreferredFormatOfsFallback() throws Exception {
        MockHttpSession session = admin();
        addTestAssets("/video");
        refreshIndex();

        PagedList<Document> assets = indexService.getAll(Pager.first());
        Document asset = assets.get(0);

        ObjectFile f = ofs.prepare("asset", assets.get(0).getAttr("source.path"), "webm");
        f.store(new FileInputStream(asset.getAttr("source.path", String.class)));

        indexService.update(asset.getId(), ImmutableMap.of("proxies",
            ImmutableMap.of("video", ImmutableList.of(new Proxy()
                    .setId(f.getId()).setFormat("webm")))));
        refreshIndex();

        asset = indexService.get(asset.getId());
        AssetController.StreamFile file = assetController.getPreferredFormat(asset,
                "webm", false, false);
        assertNotNull(file);
    }
}
