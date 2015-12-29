package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.TestSearchResult;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.IngestExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FolderControllerTests extends MockMvcTest {

    @Autowired
    FolderController folderController;

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    FolderService folderService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    Folder folder;

    @Before
    public void init() throws Exception {
        MockHttpSession session = user();

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder().setName("TestFolder1"))))
                .andExpect(status().isOk())
                .andReturn();

        folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
    }

    @Test
    public void testCreate() throws Exception {
        MockHttpSession session = user();
        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder().setName("TestFolder2"))))
                .andExpect(status().isOk())
                .andReturn();
        Folder folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertEquals("TestFolder2", folder.getName());
    }

    @Test
    public void testGet() throws Exception {
        MockHttpSession session = user();
        MvcResult result = mvc.perform(get("/api/v1/folders/" + folder.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Folder folder2 = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        assertEquals(folder.getId(), folder2.getId());
        assertEquals(folder.getParentId(), folder2.getParentId());
        assertEquals(folder.getUserCreated(), folder2.getUserCreated());
        assertEquals(folder.getName(), folder2.getName());
    }

    @Test
    public void testGetByPath() throws Exception {
        MockHttpSession session = user();
        MvcResult result = mvc.perform(get("/api/v1/folders/_/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Folder folder2 = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        assertEquals("users", folder2.getName());
        assertEquals(Folder.ROOT_ID, folder2.getParentId());
    }

    @Test
    public void testGetAll() throws Exception {
        MockHttpSession session = user();
        MvcResult result = mvc.perform(get("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Folder> folders= Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Folder>>() {});
        assertTrue(folders.contains(folder));
    }

    @Test
    public void testUpdate() throws Exception {
        MockHttpSession session = user();

        FolderBuilder builder = new FolderBuilder(folder).setName("TestFolder9000");
        MvcResult result = mvc.perform(put("/api/v1/folders/" + folder.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(builder)))
                .andExpect(status().isOk())
                .andReturn();

        Folder folder2 = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertEquals(builder.getName(), folder2.getName());
        assertEquals(folder.getParentId(), folder2.getParentId());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpSession session = user();

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder("first"))))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder("second"))))
                .andExpect(status().isOk())
                .andReturn();

        Folder second = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        mvc.perform(delete("/api/v1/folders/" + second.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        result = mvc.perform(get("/api/v1/folders")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        List<Folder> folders = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Folder>>() {});

        // This is 4 because of the folder created by init and the user folder
        assertEquals(3, folders.size());
        Set<String> names = folders.stream().map(Folder::getName).collect(Collectors.toSet());

        assertTrue(names.contains("first"));
        assertFalse(names.contains("second"));
        assertTrue(names.contains(folder.getName()));
    }

    @Test
    public void testChildren() throws Exception {
        MockHttpSession session = user();

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder("grandpa"))))
                .andExpect(status().isOk())
                .andReturn();

        Folder grandpa = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder("daddy", grandpa))))
                .andExpect(status().isOk())
                .andReturn();

        Folder dad = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder("uncly", grandpa))))
                .andExpect(status().isOk())
                .andReturn();

        result = mvc.perform(get("/api/v1/folders/" + grandpa.getId() + "/_children")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Folder> folders = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Folder>>() {});

        assertEquals(2, folders.size());
        assertEquals("daddy", folders.get(0).getName());
        assertEquals("uncly", folders.get(1).getName());

        result = mvc.perform(put("/api/v1/folders/" + dad.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(new FolderBuilder(dad).setName("daddy2"))))
                .andExpect(status().isOk())
                .andReturn();
        Folder dad2 = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        assertEquals(grandpa.getId(), dad2.getParentId().intValue());
        assertEquals(dad.getId(), dad2.getId());
    }

    @Test
    public void testAddAsset() throws Exception {
        authenticate();
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("standard")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();
        List<Asset> assets = assetDao.getAll();

        Folder folder1 = folderService.create(new FolderBuilder("foo"));

        MockHttpSession session = admin();
        mvc.perform(post("/api/v1/folders/" + folder1.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map(Asset::getId).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn();

        refreshIndex();

        assets = assetDao.getAll();
        for (Asset asset: assets) {
            Set<Integer> folderIds = asset.getValue("folders", new TypeReference<Set<Integer>>() {});
            assertTrue(folderIds.contains(folder1.getId()));
        }
    }

    @Test
    public void testRemoveAsset() throws Exception {
        authenticate();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("standard")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();
        List<Asset> assets = assetDao.getAll();

        Folder folder1 = folderService.create(new FolderBuilder("foo"));
        for (Asset asset: assets) {
            assetDao.addToFolder(asset, folder1);
        }
        refreshIndex();

        MockHttpSession session = admin();
        mvc.perform(delete("/api/v1/folders/" + folder1.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map(Asset::getId).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn();

        refreshIndex();
        assets = assetDao.getAll();
        for (Asset asset: assets) {
            Set<Integer> folderIds = asset.getValue("folders", new TypeReference<Set<Integer>>() {});
            assertFalse(folderIds.contains(folder1.getId()));
        }
    }

    @Test
    public void testGetAssets() throws Exception {
        authenticate();

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("standard")));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();
        List<Asset> assets = assetDao.getAll();

        Folder folder1 = folderService.create(new FolderBuilder("foo"));
        for (Asset asset: assets) {
            assetDao.addToFolder(asset, folder1);
        }
        refreshIndex();

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/folders/" + folder1.getId() + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        TestSearchResult searchResult = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), TestSearchResult.class);
        assertEquals(assets.size(), searchResult.getHits().getTotal());
    }
}
