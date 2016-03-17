package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteBuilder;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.common.repository.AssetDao;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteServiceTests extends ArchivistApplicationTests {

    @Autowired
    NoteService noteService;

    @Autowired
    AssetDao assetDao;

    String assetId;

    @Before
    public void init() {
        AssetBuilder ab = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetId = assetDao.upsert(ab).getId();
    }

    @Test
    public void create() {

        NoteBuilder nb = new NoteBuilder();
        nb.setTags(Sets.newHashSet("bing", "bong"));
        nb.setText("a test note");
        nb.setAssets(ImmutableSet.of(assetId));

        Note note = noteService.create(nb);
        assertTrue(note.getTags().contains("bing"));
        assertTrue(note.getTags().contains("bong"));
        assertEquals(note.getText(), nb.getText());
        assertTrue(note.getAssets().contains(assetId));
    }

    @Test
    public void createWithHashTag() {
        NoteBuilder nb = new NoteBuilder();
        nb.setTags(Sets.newHashSet("bing", "bong"));
        nb.setText("a test note #yolo #trump");
        nb.setAssets(ImmutableSet.of(assetId));

        Note note = noteService.create(nb);
        assertTrue(note.getTags().contains("bing"));
        assertTrue(note.getTags().contains("bong"));
        assertTrue(note.getTags().contains("yolo"));
        assertTrue(note.getTags().contains("trump"));
    }
}
