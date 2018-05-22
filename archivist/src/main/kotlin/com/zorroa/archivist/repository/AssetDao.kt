package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.elastic.AbstractElasticDao
import com.zorroa.archivist.elastic.SearchBuilder
import com.zorroa.archivist.elastic.SearchHitRowMapper
import com.zorroa.archivist.elastic.SingleHit
import com.zorroa.sdk.domain.AssetIndexResult
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
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
    fun getAll(page: Pager, search: SearchBuilder): PagedList<Document>

    @Throws(IOException::class)
    fun getAll(page: Pager, search: SearchBuilder, stream: OutputStream)

    /**
     * Get all assets by page.
     *
     * @param page
     * @return
     */
    fun getAll(page: Pager): PagedList<Document>

    fun getManagedFields(id: String): Map<String, Any>

    fun exists(path: Path): Boolean

    fun exists(id: String): Boolean

    operator fun get(path: Path): Document

    fun removeLink(typeOfLink: String, value: Any, assets: List<String>): Map<String, List<Any>>

    fun appendLink(typeOfLink: String, value: Any, assets: List<String>): Map<String, List<Any>>

    fun setLinks(assetId: String, type:String, ids: Collection<Any>)

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
    override val type = "asset"
    override val index = "archivist"

    override fun <T> getFieldValue(id: String, field: String): T {
        val req = GetRequest(index, type, id).fetchSourceContext(FetchSourceContext.FETCH_SOURCE)
        val d = Document(client.get(req).source)
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
        val bulkRequest = BulkRequest()

        /**
         * Force a refresh if we haven't for a while.
         */
        val time = System.currentTimeMillis()
        if (refresh || time - flushTime.getAndSet(time) > 30000) {
            bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        }

        for (source in sources) {
            if (source.isReplace) {
                bulkRequest.add(prepareInsert(source))
            } else {
                bulkRequest.add(prepareUpsert(source))
            }
        }

        val bulk = client.bulk(bulkRequest)

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
                    DocWriteRequest.OpType.UPDATE -> {
                        val update = response.getResponse<UpdateResponse>()
                        if (update.result == DocWriteResponse.Result.CREATED) {
                            result.created++
                        } else {
                            result.updated++
                        }
                        result.addToAssetIds(update.id)
                    }
                    DocWriteRequest.OpType.INDEX -> {
                        val idxr = response.getResponse<IndexResponse>()
                        if (idxr.result == DocWriteResponse.Result.CREATED) {
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

    private fun prepareUpsert(source: Document): UpdateRequest {
        val upd = UpdateRequest(index, source.type, source.id)
                .docAsUpsert(true)
                .doc(Json.serialize(source.document), XContentType.JSON)
        if (source.parentId != null) {
            upd.parent(source.parentId)
        }
        return upd
    }

    private fun prepareInsert(source: Document): IndexRequest {

        val idx = IndexRequest(index, source.type, source.id)
                .opType( DocWriteRequest.OpType.INDEX)
                .source(Json.serialize(source.document), XContentType.JSON)
        if (source.parentId != null) {
            idx.parent(source.parentId)
        }
        return idx
    }

    private fun removeBrokenField(asset: Document, error: String): Boolean {
        for (pattern in RECOVERABLE_BULK_ERRORS) {
            logger.info("checking pattern: {}", pattern)
            val matcher = pattern.matcher(error)
            if (matcher.find()) {
                logger.warn("Removing broken field from {}: {}={}, {}", asset.id, matcher.group(1),
                        asset.getAttr(matcher.group(1)), error)
                return asset.removeAttr(matcher.group(1))
            }
        }
        return false
    }

    override fun removeLink(typeOfLink: String, value: Any, assets: List<String>): Map<String, List<Any>> {
        if (typeOfLink.contains(".")) {
            throw IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)")
        }

        val link = mapOf<String,Any>("type" to typeOfLink, "id" to value.toString())
        val bulkRequest = BulkRequest()
        for (id in assets) {

            val updateBuilder = UpdateRequest(index, type, id)
            updateBuilder.script(Script(ScriptType.INLINE,
                    "painless", REMOVE_LINK_SCRIPT, link))
            bulkRequest.add(updateBuilder)
        }

        val result = mutableMapOf<String, MutableList<Any>>()
        result["success"] = mutableListOf()
        result["failed"] = mutableListOf()

        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        val bulk = client.bulk(bulkRequest)
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

    override fun appendLink(typeOfLink: String, value: Any, assets: List<String>): Map<String, List<Any>> {
        if (typeOfLink.contains(".")) {
            throw IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)")
        }
        val link = mapOf<String,Any>("type" to typeOfLink, "id" to value.toString())

        val bulkRequest = BulkRequest()
        for (id in assets) {
            val update = UpdateRequest(index, "asset", id)
            update.script(Script(ScriptType.INLINE, "painless", APPEND_LINK_SCRIPT, link))
            bulkRequest.add(update)
        }

        val result = mutableMapOf<String, MutableList<Any>>(
                "success" to mutableListOf(), "failed" to mutableListOf())

        val bulk = client.bulk(bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.NONE))
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

    override fun setLinks(assetId: String, typeOfLink:String, ids: Collection<Any>) {
        if (type.contains(".")) {
            throw IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)")
        }
        val doc = mapOf("zorroa" to mapOf("links" to mapOf(typeOfLink to ids)))
        client.update(UpdateRequest(index, type, assetId).
                doc(Json.serializeToString(doc), XContentType.JSON))
    }


    override fun update(assetId: String, attrs: Map<String, Any>): Long {
        val asset = get(assetId)
        for ((key, value) in attrs) {
            asset.setAttr(key, value)
        }

        return client.update(UpdateRequest(index, type, assetId)
                .doc(Json.serializeToString(asset.document), XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)).version
    }

    override fun removeFields(assetId: String, fields: Set<String>, refresh: Boolean) {
        val asset = get(assetId)
        for (a in fields) {
            asset.removeAttr(a)
        }

        // Replaces entire asset
        // Can't edit elements so no need for parent handling.
        client.index(IndexRequest(index, asset.type, asset.id)
                .opType(DocWriteRequest.OpType.INDEX)
                .source(asset.document)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE))
    }

    override fun delete(id: String): Boolean {
        return client.delete(DeleteRequest(index, type, id)).result == DocWriteResponse.Result.DELETED
    }

    override fun get(id: String): Document {
        return elastic.queryForObject(id, type, MAPPER)
    }

    override fun getManagedFields(id: String): Map<String, Any> {
        return try {
            /*
             * Have to use a search here because using the get API
             * with fields will fail if the asset doesn't have the
             * fields.
             */
            val result = client.get(GetRequest(index, type, id)).source
            result ?: mutableMapOf() // result has to be mutable.
        } catch (e: ArrayIndexOutOfBoundsException) {
            mutableMapOf()
        }

    }

    override fun exists(path: Path): Boolean {
        val req = SearchRequest(index)
        val source = SearchSourceBuilder()
        source.query(QueryBuilders.termQuery("source.path.raw", path.toString()))
        source.size(0)
        req.source(source)

        return client.search(req).hits.totalHits > 0
    }

    override fun exists(id: String): Boolean {
        return client.get(GetRequest(index, "asset", id)
                .fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE)).isExists
    }

    override fun get(path: Path): Document {

        val req = SearchBuilder(index)
        req.source.query(QueryBuilders.termQuery("source.path.raw", path.toString()))
        req.source.size(1)

        val assets = elastic.query(req, MAPPER)

        if (assets.isEmpty()) {
            throw EmptyResultDataAccessException("Asset $path does not exist", 1);
        }
        return assets[0]
    }

    override fun getAll(scrollId: String, timeout: String): PagedList<Document> {
        return elastic.scroll(scrollId, timeout, MAPPER)
    }

    override fun getAll(page: Pager, search: SearchBuilder): PagedList<Document> {
        return elastic.page(search, page, MAPPER)
    }

    @Throws(IOException::class)
    override fun getAll(page: Pager, search: SearchBuilder, stream: OutputStream) {
        elastic.page(search, page, stream)
    }

    override fun getAll(page: Pager): PagedList<Document> {
        val req = SearchBuilder(index)
        req.request.apply {
            types(type)
            searchType(SearchType.DFS_QUERY_THEN_FETCH)
        }
        req.source.apply {
            version(true)
            query(QueryBuilders.matchAllQuery())
        }

        return elastic.page(req, page, MAPPER)
    }

    override fun getMapping(): Map<String, Any> {
        return mapOf()
    }

    companion object {

        private const val REMOVE_LINK_SCRIPT =
                "if (ctx._source.zorroa == null) { return; } " +
                "if (ctx._source.zorroa.links == null) { return; } " +
                "if (ctx._source.zorroa.links[params.type] == null) { return; } " +
                "ctx._source.zorroa.links[params.type].removeIf({i-> i==params.id})"

        private const val APPEND_LINK_SCRIPT =
                "if (ctx._source.zorroa == null) { ctx._source.zorroa = new HashMap(); } " +
                "if (ctx._source.zorroa.links == null) { ctx._source.zorroa.links = new HashMap() }  " +
                "if (ctx._source.zorroa.links[params.type] == null) { ctx._source.zorroa.links[params.type] = new ArrayList(); }" +
                "ctx._source.zorroa.links[params.type].add(params.id); "+
                "ctx._source.zorroa.links[params.type] = new HashSet(ctx._source.zorroa.links[params.type]);"

        private val MAPPER = object : SearchHitRowMapper<Document> {
            override fun mapRow(hit: SingleHit): Document {
                val doc = Document()
                doc.document = hit.source
                doc.id = hit.id
                doc.score = hit.score
                doc.type = hit.type
                return doc
            }
        }

        private val RECOVERABLE_BULK_ERRORS = arrayOf(
                Pattern.compile("reason=failed to parse \\[(.*?)\\]"),
                Pattern.compile("\"term in field=\"(.*?)\"\""),
                Pattern.compile("mapper \\[(.*?)\\] of different type"))
    }
}
