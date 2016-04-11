package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Note;
import com.zorroa.archivist.sdk.domain.NoteBuilder;
import com.zorroa.archivist.sdk.domain.NoteSearch;

import java.util.List;

/**
 * Created by chambers on 3/16/16.
 */
public interface NoteDao {

    /**
     * Create a note.
     *
     * @param note
     * @return
     */
    Note create(NoteBuilder note);

    /**
     * Get a note.
     *
     * @param id
     * @return
     */
    Note get(String id);


    List<Note> getAll(String assetId);

    List<Note> search(NoteSearch search);
}
