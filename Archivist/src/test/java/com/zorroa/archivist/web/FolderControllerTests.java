package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Folder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FolderControllerTests extends MockMvcTest {

    @Autowired
    FolderController folderController;

    @Test
    public void testCreateAndGetAndUpdate() throws Exception {
        MockHttpSession session = user();

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"Behind The Couch\", \"userId\": 1 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Folder folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertEquals("Behind The Couch", folder.getName());

        result = mvc.perform(get("/api/v1/folders/" + folder.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertEquals("Behind The Couch", folder.getName());

        result = mvc.perform(put("/api/v1/folders/" + folder.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\" : \"Between the Cushions\", \"userId\" : 2 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();
        folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertEquals("Between the Cushions", folder.getName());
        assertEquals(2, folder.getUserId());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpSession session = user();

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"first\", \"userId\": 8 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"second\", \"userId\": 8 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Folder folder = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        mvc.perform(delete("/api/v1/folders/" + folder.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        result = mvc.perform(get("/api/v1/folders?userId=8")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();
        List<Folder> folders = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Folder>>() {});
        assertEquals(1, folders.size());
        assertEquals("first", folders.get(0).getName());
    }

    @Test
    public void testGetByUser() throws Exception {
        MockHttpSession session = user();

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"User1\", \"userId\": 6 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"User2\", \"userId\": 7 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result = mvc.perform(get("/api/v1/folders?userId=7")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();
        List<Folder> folders = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Folder>>() {});
        assertEquals(1, folders.size());
        assertEquals("User2", folders.get(0).getName());
    }

    @Test
    public void testChildren() throws Exception {
        MockHttpSession session = user();

        MvcResult result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"name\": \"grandpapa\", \"userId\": 1 }".getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Folder grandpa = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});

        String body = "{ \"name\": \"daddy\", \"userId\": 1, \"parentId\": \"" + grandpa.getId() + "\" }";
        result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body.getBytes()))
                .andExpect(status().isOk())
                .andReturn();

        Folder dad = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertNotEquals(null, dad.getParentId());

        body = "{ \"name\": \"uncly\", \"userId\": 1, \"parentId\": \"" + grandpa.getId() + "\" }";
        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body.getBytes()))
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

        body = "{ \"name\": \"daddy2\", \"userId\": 1 }";
        result = mvc.perform(put("/api/v1/folders/" + dad.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body.getBytes()))
                .andExpect(status().isOk())
                .andReturn();
        Folder dad2 = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Folder>() {});
        assertEquals(null, dad2.getParentId());
    }
}
