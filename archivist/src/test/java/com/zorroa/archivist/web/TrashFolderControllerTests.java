package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.TrashedFolder;
import com.zorroa.archivist.domain.TrashedFolderOp;
import com.zorroa.archivist.repository.TrashFolderDao;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 12/5/16.
 */
public class TrashFolderControllerTests extends MockMvcTest {

    TrashedFolderOp deleteOp;

    @Autowired
    TrashFolderDao trashFolderDao;

    Folder folder1;

    @Before
    public void init() {
        folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder2 = folderService.create(new FolderSpec("test2", folder1));
        folderService.create(new FolderSpec("test3", folder2));
        deleteOp = folderService.trash(folder1);
    }

    @Test
    public void testRestore() throws Exception {
        int exists = jdbc.queryForObject("SELECT COUNT(1) FROM folder WHERE str_name='test1'", Integer.class);
        assertEquals(0, exists);

        String content = Json.serializeToString(
                ImmutableList.of(deleteOp.getTrashFolderId().toString()));
        MvcResult result = mvc.perform(post("/api/v1/trash/_restore")
                .session(admin())
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> data = deserialize(result, Map.class);
        assertTrue((Boolean) data.get("success"));
    }

    @Test
    public void testEmpty() throws Exception {
        int exists = jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Integer.class);
        assertEquals(3, exists);

        MvcResult result = mvc.perform(delete("/api/v1/trash")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = deserialize(result, Map.class);
        assertTrue((Boolean) data.get("success"));

        List<TrashedFolder> folders = trashFolderDao.getAll(admin().getId());
        assertEquals(0, folders.size());
    }

    @Test
    public void testEmptySpecificFolders() throws Exception {
        int exists = jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Integer.class);
        assertEquals(3, exists);

        MvcResult result = mvc.perform(delete("/api/v1/trash")
                .session(admin())
                .content(Json.serializeToString(Lists.newArrayList(folder1.getId())))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> data = deserialize(result, Map.class);
        assertTrue((Boolean) data.get("success"));

        List<TrashedFolder> folders = trashFolderDao.getAll(admin().getId());
        assertEquals(0, folders.size());
    }


    @Test
    public void testGetAll() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/trash")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<TrashedFolder> data = deserialize(result, new TypeReference<List<TrashedFolder>>() {});
        assertEquals(1, data.size());
    }

    @Test
    public void testCount() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/trash/_count")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map data = deserialize(result, Map.class);
        assertEquals(1, data.get("count"));
    }
}
