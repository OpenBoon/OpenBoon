package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.Note;
import com.zorroa.archivist.sdk.domain.NoteBuilder;
import com.zorroa.archivist.sdk.domain.NoteSearch;

import java.util.List;

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
