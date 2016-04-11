package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.Note;
import com.zorroa.archivist.sdk.domain.NoteBuilder;
import com.zorroa.archivist.sdk.domain.NoteSearch;
import com.zorroa.archivist.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by chambers on 3/17/16.
 */
@RestController
public class NoteController {

    @Autowired
    NoteService noteService;

    /**
     * Getting all notes from an asset is in AssetController
     */

    /**
     * Get a particular note.
     */
    @RequestMapping(value="/api/v1/notes/{id}", method=RequestMethod.GET)
    public Note get(@PathVariable String id) {
        return noteService.get(id);
    }

    /**
     * Create a new note.
     */
    @RequestMapping(value="/api/v1/notes", method=RequestMethod.POST)
    public Note create(@RequestBody NoteBuilder builder) {
        return noteService.create(builder);
    }

    /**
     * Search for note.
     */
    @RequestMapping(value="/api/v1/notes/_search", method=RequestMethod.POST)
    public List<Note> search(@RequestBody NoteSearch search) {
        return noteService.search(search);
    }
}
