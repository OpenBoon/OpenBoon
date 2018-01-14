package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Note
import com.zorroa.archivist.domain.NoteSpec
import com.zorroa.archivist.service.NoteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class NoteController @Autowired constructor(
        private val noteService: NoteService
){

    /**
     * Get a particular note.
     */
    @GetMapping(value = "/api/v1/notes/{id}")
    fun get(@PathVariable id: String): Note {
        return noteService[id]
    }

    /**
     * Create a new note.
     */
    @PostMapping(value = "/api/v1/notes")
    fun create(@RequestBody builder: NoteSpec): Note {
        return noteService.create(builder)
    }
}
