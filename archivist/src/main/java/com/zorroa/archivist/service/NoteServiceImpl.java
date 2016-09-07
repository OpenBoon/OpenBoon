package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;
import com.zorroa.archivist.repository.NoteDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by chambers on 3/16/16.
 */
@Service
public class NoteServiceImpl implements NoteService {

    private static final Pattern TAG_PATTERN = Pattern.compile("#(\\w+)", Pattern.CASE_INSENSITIVE);

    /**
     * Will be used to auto-complete names and notify a person mentioned in the note.
     */
    private static final Pattern USER_PATTERN = Pattern.compile("@(\\w+)", Pattern.CASE_INSENSITIVE);

    @Autowired
    NoteDao noteDao;

    @Override
    public Note create(NoteSpec builder) {
        Preconditions.checkNotNull(builder.getText(), "Text of a note cannot be null");
        Preconditions.checkNotNull(builder.getAsset(), "Asset cannot be null");
        return noteDao.create(builder);
    }

    @Override
    public Note get(String id) {
        return noteDao.get(id);
    }

    @Override
    public List<Note> getAll(String assetId) {
        return noteDao.getAll(assetId);
    }
}
