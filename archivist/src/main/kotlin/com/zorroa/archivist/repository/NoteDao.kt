package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.Note
import com.zorroa.archivist.domain.NoteSpec
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.common.elastic.AbstractElasticDao
import com.zorroa.common.elastic.SearchHitRowMapper
import com.zorroa.sdk.util.Json
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

interface NoteDao {

    fun create(spec: NoteSpec): Note

    fun get(id: String): Note

    fun getAll(assetId: String): List<Note>
}

/**
 * Created by chambers on 3/16/16.
 */
@Repository
open class NoteDaoImpl : AbstractElasticDao(), NoteDao {

    @Autowired
    internal var userDaoCache: UserDaoCache? = null

    private val MAPPER = SearchHitRowMapper<Note> { hit ->
        val note = Json.Mapper.convertValue(hit.source, Note::class.java)
        note.id = hit.id
        note.user = userDaoCache!!.getUser(note.userId)
        note
    }

    override fun create(spec: NoteSpec): Note {
        val map = ImmutableMap.builder<String, Any>()
                .put("text", spec.text)
                .put("asset", spec.asset)
                .put("timeCreated", System.currentTimeMillis())
                .put("userId", SecurityUtils.getUser().id)

        val rsp = client.prepareIndex(index, type)
                .setSource(Json.serializeToString(map.build()))
                .setRefresh(true)
                .get()

        return get(rsp.id)
    }

    override fun get(id: String): Note {
        return elastic.queryForObject(id, MAPPER)
    }

    override fun getAll(assetId: String): List<Note> {
        return elastic.query(client.prepareSearch(index)
                .setTypes(type)
                .setQuery(QueryBuilders.termQuery("asset", assetId))
                .addSort(SortBuilders.fieldSort("timeCreated"))
                .setSize(1000),
                MAPPER)
    }

    override fun getType(): String {
        return "note"
    }

    override fun getIndex(): String {
        return "notes"
    }
}
