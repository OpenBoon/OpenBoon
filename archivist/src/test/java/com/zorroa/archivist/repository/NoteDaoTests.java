package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteDaoTests extends AbstractTest {

    @Autowired
    NoteDao noteDao;

    String assetId;

    @Before
    public void init() {
        Source ab = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        assetId = assetService.index(ab).getId();
    }

    @Test
    public void create() {
        NoteBuilder nb = new NoteBuilder();
        nb.setTags(ImmutableSet.of("foo", "bar"));
        nb.setText("a note for the king");
        nb.setAsset(assetId);

        Note note = noteDao.create(nb);
        assertTrue(note.getTags().contains("foo"));
        assertTrue(note.getTags().contains("bar"));
        assertEquals(note.getText(), nb.getText());
        assertEquals(note.getAsset(), assetId);
    }

    @Test
    public void createWithAnnotation() {
        NoteBuilder nb = new NoteBuilder();
        nb.setText("an annotated note!");
        nb.addToAnnotations(new Annotation().addToGeometries(
                new Shape("point").setCoordinates(ImmutableList.of(1.0, 1.0))));
        nb.setAsset(assetId);

        Note note = noteDao.create(nb);
        assertEquals(note.getText(), nb.getText());
        assertEquals(note.getAsset(), assetId);

        Annotation annotation = note.getAnnotations().get(0);
        assertEquals("point", annotation.getGeometries().get(0).getType());
    }

    @Test
    public void get() {
        NoteBuilder nb = new NoteBuilder();
        nb.setTags(ImmutableSet.of("foo", "bar"));
        nb.setText("a note for the king");
        nb.setAsset(assetId);

        Note note1 = noteDao.create(nb);
        Note note2 = noteDao.get(note1.getId());
        assertEquals(note1.getId(), note2.getId());
    }

    @Test
    public void getAll() {

        for (int i=0; i<10; i++) {
            NoteBuilder nb = new NoteBuilder();
            nb.setTags(ImmutableSet.of("foo"+i, "bar"+i));
            nb.setText("note number " + i);
            nb.setAsset(assetId);
            noteDao.create(nb);
        }

        List<Note> notes = noteDao.getAll(assetId);
        assertEquals(10, notes.size());
    }


    @Test
    public void search() {

        for (int i=0; i<10; i++) {
            NoteBuilder nb = new NoteBuilder();
            nb.setTags(ImmutableSet.of("foo"+i, "bar"+i));
            nb.setText("happy birthday mr president" + i);
            nb.setAsset(assetId);
            noteDao.create(nb);
        }

        refreshIndex();

        List<Note> notes = noteDao.getAll(assetId);
        assertEquals(10, notes.size());

        notes = noteDao.search(new NoteSearch().setQuery("president"));
        assertEquals(10, notes.size());
    }
}
