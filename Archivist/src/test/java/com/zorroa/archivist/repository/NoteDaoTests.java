package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteBuilder;
import com.zorroa.archivist.domain.NoteSearch;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.common.repository.AssetDao;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteDaoTests extends ArchivistApplicationTests {

    @Autowired
    NoteDao noteDao;

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
        nb.setTags(ImmutableSet.of("foo", "bar"));
        nb.setText("a note for the king");
        nb.setAssets(ImmutableSet.of(assetId));

        Note note = noteDao.create(nb);
        assertTrue(note.getTags().contains("foo"));
        assertTrue(note.getTags().contains("bar"));
        assertEquals(note.getText(), nb.getText());
        assertTrue(note.getAssets().contains(assetId));
    }

    @Test
    public void get() {
        NoteBuilder nb = new NoteBuilder();
        nb.setTags(ImmutableSet.of("foo", "bar"));
        nb.setText("a note for the king");
        nb.setAssets(ImmutableSet.of(assetId));

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
            nb.setAssets(ImmutableSet.of(assetId));
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
            nb.setAssets(ImmutableSet.of(assetId));
            noteDao.create(nb);
        }

        refreshIndex();

        List<Note> notes = noteDao.getAll(assetId);
        assertEquals(10, notes.size());

        notes = noteDao.search(new NoteSearch().setQuery("president"));
        assertEquals(10, notes.size());
    }
}
