package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;

import java.util.List;

public interface NoteService {
    /**
     * Create and return a note.
     *
     * @param builder
     * @return
     */
    Note create(NoteSpec builder);

    Note get(String id);

    List<Note> getAll(String assetId);
}
