package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteBuilder;
import com.zorroa.archivist.domain.NoteSearch;

import java.util.List;

/**
 * Created by chambers on 3/16/16.
 */
public interface NoteService {
    /**
     * Create and return a note.
     *
     * @param builder
     * @return
     */
    Note create(NoteBuilder builder);

    Note get(String id);

    List<Note> getAll(String assetId);

    List<Note> search(NoteSearch search);
}
