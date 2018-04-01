package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.common.elastic.AbstractElasticDao
import com.zorroa.common.elastic.SearchHitRowMapper
import com.zorroa.sdk.client.exception.ArchivistException
import com.zorroa.sdk.domain.AssetIndexResult
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.update.UpdateRequestBuilder
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptService
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Repository
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

interface AssetDao {

    fun getMapping(): Map<String, Any>

    fun removeFields(assetId: String, fields: Set<String>, refresh: Boolean)

    fun delete(id: String): Boolean

    operator fun get(id: String): Document

    /**
     * Return the next page of an asset scroll.
     *
     * @param scrollId
     * @param timeout
     * @return
     */
    fun getAll(scrollId: String, timeout: String): PagedList<Document>

    /**
     * Get all assets given the page and SearchRequestBuilder.
     *
     * @param page
     * @param search
     * @return
     */
    fun getAll(page: Pager, search: SearchRequestBuilder): PagedList<Document>

    @Throws(IOException::class)
    fun getAll(page: Pager, search: SearchRequestBuilder, stream: OutputStream, attrs: Map<String, Any>)

    /**
     * Get all assets by page.
     *
     * @param page
     * @return
     */
    fun getAll(page: Pager): PagedList<Document>

    operator fun get(id: String, type: String, parent: String): Document

    fun getManagedFields(id: String): Map<String, Any>

    fun exists(path: Path): Boolean

    fun exists(id: String): Boolean

    operator fun get(path: Path): Document

    fun removeLink(type: String, value: Any, assets: List<String>): Map<String, List<Any>>

    fun appendLink(type: String, value: Any, assets: List<String>): Map<String, List<Any>>

    fun update(assetId: String, attrs: Map<String, Any>): Long

    fun <T> getFieldValue(id: String, field: String): T

    fun index(source: Document): Document

    /**
     * Index the given sources.  If any assets are created, attach a source link.
     * @param sources
     * @return
     */
    fun index(sources: List<Document>): AssetIndexResult

    fun index(sources: List<Document>, refresh: Boolean): AssetIndexResult
}

@Repository
class AssetDaoImpl : AbstractElasticDao(), AssetDao {

    /**
     * Allows us to flush the first batch.
     */
    private val flushTime = AtomicLong(0)

    override fun getType(): String {
        return "asset"
    }

    override fun getIndex(): String {
        return "archivist"
    }

    override fun <T> getFieldValue(id: String, field: String): T {
        val d = Document(
                client.prepareGet("archivist", "asset", id)
                        .get().source)
        // field values never have .raw since they come from source
        return d.getAttr(field.removeSuffix(".raw"))
    }

    override fun index(source: Document): Document {
        index(ImmutableList.of(source), true)
        return get(source.id)
    }

    override fun index(sources: List<Document>): AssetIndexResult {
        return index(sources, false)
    }

    override fun index(sources: List<Document>, refresh: Boolean): AssetIndexResult {
        val result = AssetIndexResult()
        if (sources.isEmpty()) {
            return result
        }

        val retries = Lists.newArrayList<Document>()
        val bulkRequest = client.prepareBulk()

        /**
         * Force a refresh if we haven't for a while.
         */
        val time = System.currentTimeMillis()
        if (refresh || time - flushTime.getAndSet(time) > 30000) {
            bulkRequest.setRefresh(true)
        }

        for (source in sources) {
            if (source.isReplace) {
                bulkRequest.add(prepareInsert(source))
            } else {
                bulkRequest.add(prepareUpsert(source))
            }
        }

        val bulk = bulkRequest.get()

        for ((index, response) in bulk.withIndex()) {
            if (response.isFailed) {
                val message = response.failure.message
                val asset = sources[index]
                if (removeBrokenField(asset, message)) {
                    result.warnings++
                    retries.add(sources[index])
                } else {
                    logger.warn("Failed to index {}, {}", response.id, message)
                    result.logs.add(StringBuilder(1024).append(
                            message).append(",").toString())
                    result.errors++
                }
            } else {
                when (response.opType) {
                    "update" -> {
                        val update = response.getResponse<UpdateResponse>()
                        if (update.isCreated) {
                            result.created++
                        } else {
                            result.updated++
                        }
                        result.addToAssetIds(update.id)
                    }
                    "index" -> {
                        val idxr = response.getResponse<IndexResponse>()
                        if (idxr.isCreated) {
                            result.created++
                        } else {
                            result.replaced++
                        }
                        result.addToAssetIds(idxr.id)
                    }
                }
            }
        }

        /*
         * TODO: limit number of retries to reasonable number.
         */
        if (!retries.isEmpty()) {
            result.retries++
            result.add(index(retries))
        }
        return result
    }

    private fun prepareUpsert(source: Document): UpdateRequestBuilder {
        val doc = Json.serialize(source.document)

        val upd = client.prepareUpdate(index, source.type, source.id)
                .setDoc(doc)
                .setUpsert(doc)
        if (source.parentId != null) {
            upd.setParent(source.parentId)
        }
        return upd
    }

    private fun prepareInsert(source: Document): IndexRequestBuilder {
        val doc = Json.serialize(source.document)
        val idx = client.prepareIndex(index, source.type, source.id)
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(doc)
        if (source.parentId != null) {
            idx.setParent(source.parentId)
        }
        return idx
    }

    private fun removeBrokenField(asset: Document, error: String): Boolean {
        for (pattern in RECOVERABLE_BULK_ERRORS) {
            val matcher = pattern.matcher(error)
            if (matcher.find()) {
                logger.warn("Removing broken field from {}: {}, {}", asset.id, matcher.group(1), error)
                return asset.removeAttr(matcher.group(1))
            }
        }
        return false
    }

    override fun removeLink(type: String, value: Any, assets: List<String>): Map<String, List<Any>> {
        if (type.contains(".")) {
            throw IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)")
        }

        val link = ImmutableMap.of("type", type, "id", value.toString())
        val bulkRequest = client.prepareBulk()
        for (id in assets) {
            val updateBuilder = client.prepareUpdate(index, getType(), id)
            updateBuilder.setScript(Script("remove_link",
                    ScriptService.ScriptType.INDEXED, "groovy",
                    link))
            bulkRequest.add(updateBuilder)
        }

        val result = mutableMapOf<String, MutableList<Any>>()
        result.put("success", mutableListOf())
        result.put("failed", mutableListOf())

        val bulk = bulkRequest.setRefresh(true).get()
        for (rsp in bulk.items) {
            if (rsp.isFailed) {
                result["failed"]!!.add(ImmutableMap.of("id", rsp.id, "error", rsp.failureMessage))
                logger.warn("Failed to unlink asset: {}", rsp.failureMessage, rsp.failure.cause)
            } else {
                result["success"]!!.add(rsp.id)
            }
        }
        return result
    }

    override fun appendLink(type: String, value: Any, assets: List<String>): Map<String, List<Any>> {
        if (type.contains(".")) {
            throw IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)")
        }
        val link = ImmutableMap.of("type", type, "id", value.toString())

        val bulkRequest = client.prepareBulk()
        for (id in assets) {
            val updateBuilder = client.prepareUpdate(index, getType(), id)

            updateBuilder.setScript(Script("append_link",
                    ScriptService.ScriptType.INDEXED, "groovy",
                    link))

            bulkRequest.add(updateBuilder)
        }

        val result = mutableMapOf<String, MutableList<Any>>(
                "success" to mutableListOf(), "failed" to mutableListOf())

        val bulk = bulkRequest.setRefresh(true).get()
        for (rsp in bulk.items) {
            if (rsp.isFailed) {
                result["failed"]!!.add(ImmutableMap.of("id", rsp.id, "error", rsp.failureMessage))
                logger.warn("Failed to link asset: {}", rsp.failureMessage, rsp.failure.cause)
            } else {
                result["success"]!!.add(rsp.id)
            }
        }

        return result
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Long {
        val asset = get(assetId)
        for ((key, value) in attrs) {
            asset.setAttr(key, value)
        }

        val updateBuilder = client.prepareUpdate(index, type, assetId)
                .setDoc(Json.serializeToString(asset.document))
                .setRefresh(true)

        val response = updateBuilder.get()
        return response.version
    }

    override fun removeFields(assetId: String, fields: Set<String>, refresh: Boolean) {
        val asset = get(assetId)
        for (a in fields) {
            asset.removeAttr(a)
        }

        // Replaces entire asset
        // Can't edit elements so no need for parent handling.
        val idx = client.prepareIndex(index, asset.type, asset.id)
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(asset.document)
                .setRefresh(true)
        idx.get()
    }

    override fun delete(id: String): Boolean {
        return client.prepareDelete(index, type, id).get().isFound
    }

    override fun get(id: String): Document {
        return elastic.queryForObject(id, MAPPER)
    }

    override fun get(id: String, type: String, parent: String): Document {
        return elastic.queryForObject(id, type, parent, MAPPER)
    }

    override fun getManagedFields(id: String): Map<String, Any> {
        return try {
            /*
             * Have to use a search here because using the get API
             * with fields will fail if the asset doesn't have the
             * fields.
             */
            val result = client.prepareGet(index, type, id)
                    .setFetchSource(arrayOf("zorroa"), arrayOf())
                    .get().source
            result ?: mutableMapOf() // result has to be mutable.
        } catch (e: ArrayIndexOutOfBoundsException) {
            mutableMapOf()
        }

    }

    override fun exists(path: Path): Boolean {
        return client.prepareSearch(index)
                .setFetchSource(false)
                .setQuery(QueryBuilders.termQuery("source.path.raw", path.toString()))
                .setSize(0)
                .get().hits.totalHits > 0
    }

    override fun exists(id: String): Boolean {
        return client.prepareGet(index, "asset", id).setFetchSource(false).get().isExists
    }

    override fun get(path: Path): Document {
        val assets = elastic.query(client.prepareSearch(index)
                .setTypes(type)
                .setSize(1)
                .setQuery(QueryBuilders.termQuery("source.path.raw", path.toString())), MAPPER)

        if (assets.isEmpty()) {
            throw EmptyResultDataAccessException("Asset $path does not exist", 1);
        }
        return assets[0]
    }

    override fun getAll(scrollId: String, timeout: String): PagedList<Document> {
        return elastic.scroll(scrollId, timeout, MAPPER)
    }

    override fun getAll(page: Pager, search: SearchRequestBuilder): PagedList<Document> {
        return elastic.page(search, page, MAPPER)
    }

    @Throws(IOException::class)
    override fun getAll(page: Pager, search: SearchRequestBuilder, stream: OutputStream, attrs: Map<String, Any>) {
        elastic.page(search, page, stream, attrs)
    }

    override fun getAll(page: Pager): PagedList<Document> {
        return elastic.page(client.prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .setVersion(true), page, MAPPER)

    }

    override fun getMapping(): Map<String, Any> {
        val cs = client.admin().cluster().prepareState().setIndices(
                index).execute().actionGet().state
        // Should only be one concrete index.
        for (index in cs.metaData.concreteAllOpenIndices()) {
            val imd = cs.metaData.index(index)
            val mdd = imd.mapping("asset")
            try {
                return mdd.sourceAsMap
            } catch (e: IOException) {
                throw ArchivistException(e)
            }

        }
        return mapOf()
    }

    companion object {


        private val MAPPER = SearchHitRowMapper<Document> { hit ->
            val doc = Document()
            doc.document = hit.source
            doc.id = hit.id
            doc.score = hit.score
            doc.type = hit.type
            if (hit.field("_parent") != null) {
                doc.parentId = hit.field("_parent").value()
            }

            hit.innerHits?.let {
                doc.elements = mutableListOf()
                for (ehit in hit.innerHits["element"]!!) {
                    doc.elements.add(( SearchHitRowMapper<Document> {
                        val edoc = Document()
                        edoc.document = hit.getSource()
                        edoc.id = hit.getId()
                        edoc.score = hit.getScore()
                        edoc.type = hit.getType()
                        if (ehit.field("_parent") != null) {
                            edoc.parentId = ehit.field("_parent").value()
                        }
                        edoc
                    }.mapRow(ehit)))
                }
            }
            doc
        }

        private val RECOVERABLE_BULK_ERRORS = arrayOf(
                Pattern.compile("^MapperParsingException\\[failed to parse \\[(.*?)\\]\\];"),
                Pattern.compile("\"term in field=\"(.*?)\"\""),
                Pattern.compile("mapper \\[(.*?)\\] of different type"))
    }
}
