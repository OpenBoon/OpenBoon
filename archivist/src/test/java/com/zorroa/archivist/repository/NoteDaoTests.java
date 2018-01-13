package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteDaoTests extends AbstractTest {

    @Autowired
    NoteDao noteDao;

    String assetId;

    @Before
    public void init() {
        cleanElastic();
        Source ab = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        assetId = assetService.index(ab).getId();
    }

    @Test
    public void create() {
        NoteSpec nb = new NoteSpec();
        nb.setText("a note for the king");
        nb.setAsset(assetId);

        Note note = noteDao.create(nb);
        assertEquals(note.getText(), nb.getText());
        assertEquals(note.getAsset(), assetId);
    }

    @Test
    public void get() {
        NoteSpec nb = new NoteSpec();
        nb.setText("a note for the king");
        nb.setAsset(assetId);

        Note note1 = noteDao.create(nb);
        Note note2 = noteDao.get(note1.getId());
        assertEquals(note1.getId(), note2.getId());
    }

    @Test
    public void getAll() {

        for (int i=0; i<10; i++) {
            NoteSpec nb = new NoteSpec();
            nb.setText("note number " + i);
            nb.setAsset(assetId);
            noteDao.create(nb);
        }

        List<Note> notes = noteDao.getAll(assetId);
        assertEquals(10, notes.size());
    }
}
