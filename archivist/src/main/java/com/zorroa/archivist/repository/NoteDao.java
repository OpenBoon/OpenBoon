package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;

import java.util.List;

/**
 * Created by chambers on 3/16/16.
 */
public interface NoteDao {

    Note create(NoteSpec spec);

    /**
     * Get a note.
     *
     * @param id
     * @return
     */
    Note get(String id);


    List<Note> getAll(String assetId);
}
