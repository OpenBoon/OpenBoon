package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;
import com.zorroa.archivist.service.NoteService;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

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

    String assetId;

    @Before
    public void init() {
        Source ab = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        assetId = assetService.index(ab).getId();
        refreshIndex();
    }

    @Test
    public void testCreate() throws Exception {
        NoteSpec nb = new NoteSpec();
        nb.setAsset(assetId);
        nb.setText("a note on an asset");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/notes")
                .session(session)
                .content(Json.serialize(nb))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Note n = Json.deserialize(result.getResponse().getContentAsByteArray(), Note.class);
        assertEquals(n.getText(), nb.getText());
        assertEquals(n.getAsset(), nb.getAsset());
    }

    @Test
    public void getNote() throws Exception {
        NoteSpec nb = new NoteSpec();
        nb.setAsset(assetId);
        nb.setText("a note on an asset elephant");
        Note note = noteService.create(nb);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/notes/" + note.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Note n = Json.deserialize(result.getResponse().getContentAsByteArray(), Note.class);
        assertEquals(n.getText(), nb.getText());
        assertEquals(n.getAsset(), nb.getAsset());
    }
}
