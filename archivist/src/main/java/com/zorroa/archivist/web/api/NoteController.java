package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;
import com.zorroa.archivist.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Note create(@RequestBody NoteSpec builder) {
        return noteService.create(builder);
    }
}
