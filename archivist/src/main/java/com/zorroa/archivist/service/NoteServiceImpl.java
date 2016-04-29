package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.zorroa.archivist.repository.NoteDao;
import com.zorroa.sdk.domain.Note;
import com.zorroa.sdk.domain.NoteBuilder;
import com.zorroa.sdk.domain.NoteSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
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
    public Note create(NoteBuilder builder) {
        Preconditions.checkNotNull(builder.getText(), "Text of a note cannot be null");

        /*
         * Look for any hash tags
         */
        Set<String> tags = builder.getTags();
        if (tags == null) {
            tags = Sets.newHashSet();
            builder.setTags(tags);
        }

        Matcher mat = TAG_PATTERN.matcher(builder.getText());
        while (mat.find()) {
            tags.add(mat.group(1));
        }

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

    @Override
    public List<Note> search(NoteSearch search) {
        return noteDao.search(search);
    }
}
