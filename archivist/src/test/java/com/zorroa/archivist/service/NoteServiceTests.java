package com.zorroa.archivist.service;

import com.google.common.collect.Sets;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.Note;
import com.zorroa.sdk.domain.NoteBuilder;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteServiceTests extends AbstractTest {

    @Autowired
    NoteService noteService;

    String assetId;

    @Before
    public void init() {
        Source ab = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        assetId = assetService.index(ab).getId();
    }

    @Test
    public void create() {

        NoteBuilder nb = new NoteBuilder();
        nb.setTags(Sets.newHashSet("bing", "bong"));
        nb.setText("a test note");
        nb.setAsset(assetId);

        Note note = noteService.create(nb);
        assertTrue(note.getTags().contains("bing"));
        assertTrue(note.getTags().contains("bong"));
        assertEquals(note.getText(), nb.getText());
        assertEquals(note.getAsset(), assetId);
    }

    @Test
    public void createWithHashTag() {
        NoteBuilder nb = new NoteBuilder();
        nb.setTags(Sets.newHashSet("bing", "bong"));
        nb.setText("a test note #yolo #trump");
        nb.setAsset(assetId);

        Note note = noteService.create(nb);
        assertTrue(note.getTags().contains("bing"));
        assertTrue(note.getTags().contains("bong"));
        assertTrue(note.getTags().contains("yolo"));
        assertTrue(note.getTags().contains("trump"));
    }
}
