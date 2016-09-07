package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

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
        NoteSpec nb = new NoteSpec();
        nb.setText("a test note");
        nb.setAsset(assetId);

        Note note = noteService.create(nb);
        assertEquals(note.getText(), nb.getText());
        assertEquals(note.getAsset(), assetId);
    }
}
