package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.archivist.service.NoteService;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Note;
import com.zorroa.sdk.domain.NoteBuilder;
import com.zorroa.sdk.domain.NoteSearch;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteControllerTests extends MockMvcTest {

    @Autowired
    NoteService noteService;

    @Autowired
    AssetDao assetDao;

    String assetId;

    @Before
    public void init() {
        Source ab = new Source(getTestImagePath("standard/beer_kettle_01.jpg"));
        assetId = assetDao.index(ab).getId();
        refreshIndex();
    }

    @Test
    public void testCreate() throws Exception {
        NoteBuilder nb = new NoteBuilder();
        nb.setAsset(assetId);
        nb.setTags(Sets.newHashSet("foo"));
        nb.setText("a note on an asset");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/notes")
                .session(session)
                .content(Json.serialize(nb))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Note n = Json.deserialize(result.getResponse().getContentAsByteArray(), Note.class);
        assertEquals(n.getTags(), nb.getTags());
        assertEquals(n.getText(), nb.getText());
        assertEquals(n.getAsset(), nb.getAsset());
    }

    @Test
    public void getSearch() throws Exception {
        NoteBuilder nb = new NoteBuilder();
        nb.setAsset(assetId);
        nb.setTags(Sets.newHashSet("foo"));
        nb.setText("a note on an asset elephant");
        noteService.create(nb);

        NoteSearch search = new NoteSearch().setQuery("foo");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/notes/_search")
                .session(session)
                .content(Json.serialize(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Note> notes = Json.deserialize(result.getResponse().getContentAsByteArray(),
                new TypeReference<List<Note>>() {});
        assertEquals(1, notes.size());
    }

    @Test
    public void getNote() throws Exception {
        NoteBuilder nb = new NoteBuilder();
        nb.setAsset(assetId);
        nb.setTags(Sets.newHashSet("foo"));
        nb.setText("a note on an asset elephant");
        Note note = noteService.create(nb);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/notes/" + note.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Note n = Json.deserialize(result.getResponse().getContentAsByteArray(), Note.class);
        assertEquals(n.getTags(), nb.getTags());
        assertEquals(n.getText(), nb.getText());
        assertEquals(n.getAsset(), nb.getAsset());
    }
}
