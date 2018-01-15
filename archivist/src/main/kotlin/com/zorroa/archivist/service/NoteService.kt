package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.Note
import com.zorroa.archivist.domain.NoteSpec
import com.zorroa.archivist.repository.NoteDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

interface NoteService {
    /**
     * Create and return a note.
     *
     * @param builder
     * @return
     */
    fun create(builder: NoteSpec): Note

    operator fun get(id: String): Note

    fun getAll(assetId: String): List<Note>
}

@Service
class NoteServiceImpl @Autowired constructor(
        private val noteDao: NoteDao
) : NoteService {

    override fun create(builder: NoteSpec): Note {
        Preconditions.checkNotNull(builder.text, "Text of a note cannot be null")
        Preconditions.checkNotNull(builder.asset, "Asset cannot be null")
        return noteDao.create(builder)
    }

    override fun get(id: String): Note {
        return noteDao.get(id)
    }

    override fun getAll(assetId: String): List<Note> {
        return noteDao.getAll(assetId)
    }

    companion object {

        private val TAG_PATTERN = Pattern.compile("#(\\w+)", Pattern.CASE_INSENSITIVE)

        /**
         * Will be used to auto-complete names and notify a person mentioned in the note.
         */
        private val USER_PATTERN = Pattern.compile("@(\\w+)", Pattern.CASE_INSENSITIVE)
    }
}
