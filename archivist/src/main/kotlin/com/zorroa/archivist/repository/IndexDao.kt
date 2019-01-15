package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.elastic.AbstractElasticDao
import com.zorroa.archivist.elastic.SearchHitRowMapper
import com.zorroa.archivist.elastic.SingleHit
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.service.event
import com.zorroa.archivist.service.warnEvent
import com.zorroa.common.clients.SearchBuilder
import com.zorroa.common.util.Json
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Repository
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

interface IndexDao {

    fun getMapping(): Map<String, Any>

    fun delete(id: String): Boolean

    /**
     * Batch delete the given asset IDs.
     * @param ids the list of asset IDS to delete.
     */
    fun batchDelete(ids: List<Document>): BatchDeleteAssetsResponse

    fun get(id: String): Document

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

    fun exists(path: Path): Boolean

    fun exists(id: String): Boolean

    fun get(path: Path): Document

    fun removeLink(typeOfLink: String, value: Any, assets: List<String>): Map<String, List<Any>>

    fun appendLink(typeOfLink: String, value: Any, assets: List<String>): Map<String, List<Any>>

    fun setLinks(assetId: String, type:String, ids: Collection<Any>)

    fun update(doc: Document): Long

    fun <T> getFieldValue(id: String, field: String): T?

    fun index(source: Document): Document

    /**
     * Index the given sources.  If any assets are created, attach a source link.
     * @param sources
     * @return
     */
    fun index(sources: List<Document>): BatchCreateAssetsResponse

    fun index(sources: List<Document>, refresh: Boolean): BatchCreateAssetsResponse
}

@Repository
class IndexDaoImpl @Autowired constructor(
        private val properties: ApplicationProperties
) : AbstractElasticDao(), IndexDao {

    /**
     * Allows us to flush the first batch.
     */
    private val flushTime = AtomicLong(0)

    override fun <T> getFieldValue(id: String, field: String): T? {
        val rest = getClient()
        val req = rest.newGetRequest(id)
                .fetchSourceContext(FetchSourceContext.FETCH_SOURCE)
        val d = Document(rest.client.get(req).source)
        // field values never have .raw since they come from source
        return d.getAttr(field.removeSuffix(".raw"))
    }

    override fun index(source: Document): Document {
        index(ImmutableList.of(source), true)
        return get(source.id!!)
    }

    override fun index(sources: List<Document>): BatchCreateAssetsResponse {
        return index(sources, false)
    }

    override fun index(sources: List<Document>, refresh: Boolean): BatchCreateAssetsResponse {
        val result = BatchCreateAssetsResponse(sources.size)
        if (sources.isEmpty()) {
            return result
        }

        val retries = Lists.newArrayList<Document>()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        for (source in sources) {
            bulkRequest.add(prepareInsert(source))
        }

        val rest = getClient()
        val bulk = rest.client.bulk(bulkRequest)

        var index = -1
        for (response in bulk.items) {
            index++
            if (response.isFailed) {
                val message = response.failure.message
                val asset = sources[index]
                if (removeBrokenField(asset, message)) {
                    result.warningAssetIds.add(asset.id)
                    retries.add(sources[index])
                } else {
                    logger.warnEvent(LogObject.ASSET, LogAction.BATCH_INDEX, message,
                            mapOf("assetId" to response.id,
                                    "index" to response.index))
                    result.erroredAssetIds.add(asset.id)
                }
            } else {
                when (response.opType) {
                    DocWriteRequest.OpType.INDEX -> {
                        val idxr = response.getResponse<IndexResponse>()
                        if (idxr.result == DocWriteResponse.Result.CREATED) {
                            result.createdAssetIds.add(idxr.id)
                        } else {
                            result.replacedAssetIds.add(idxr.id)
                        }

                    }
                }
            }
        }

        /*
         * TODO: limit number of retries to reasonable number.
         */
        if (!retries.isEmpty()) {
            result.retryCount++
            result.add(index(retries))
        }

        return result
    }

    private fun prepareUpsert(source: Document): UpdateRequest {
        val rest = getClient()
        val upd = rest.newUpdateRequest(source.id)
                .docAsUpsert(true)
                .doc(Json.serialize(source.document), XContentType.JSON)
        return upd
    }

    private fun prepareInsert(source: Document): IndexRequest {
        val rest = getClient()
        val idx = rest.newIndexRequest(source.id)
                .opType(DocWriteRequest.OpType.INDEX)
                .source(Json.serialize(source.document), XContentType.JSON)
        return idx
    }

    private fun removeBrokenField(asset: Document, error: String): Boolean {
        for (pattern in RECOVERABLE_BULK_ERRORS) {
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

        val rest = getClient()
        val link = mapOf<String,Any>("type" to typeOfLink, "id" to value.toString())
        val bulkRequest = BulkRequest()
        for (id in assets) {

            val updateBuilder = rest.newUpdateRequest(id)
            updateBuilder.script(Script(ScriptType.INLINE,
                    "painless", REMOVE_LINK_SCRIPT, link))
            bulkRequest.add(updateBuilder)
        }

        val result = mutableMapOf<String, MutableList<Any>>()
        result["success"] = mutableListOf()
        result["failed"] = mutableListOf()

        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        val bulk = rest.client.bulk(bulkRequest)
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

        val rest = getClient()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        for (id in assets) {
            val update = rest.newUpdateRequest(id)
            update.script(Script(ScriptType.INLINE, "painless", APPEND_LINK_SCRIPT, link))
            bulkRequest.add(update)
        }

        val result = mutableMapOf<String, MutableList<Any>>(
                "success" to mutableListOf(), "failed" to mutableListOf())

        val bulk = rest.client.bulk(bulkRequest)
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
        if (typeOfLink.contains(".")) {
            throw IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)")
        }
        val doc = mapOf("system" to mapOf("links" to mapOf(typeOfLink to ids)))
        val rest = getClient()
        rest.client.update(rest.newUpdateRequest(assetId)
                .doc(Json.serializeToString(doc), XContentType.JSON))
    }

    override fun update(asset: Document): Long {
        val rest = getClient()
        val ver =  rest.client.update(rest.newUpdateRequest(asset.id)
                .doc(Json.serializeToString(asset.document), XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)).version
        return ver
    }

    override fun delete(id: String): Boolean {
        val rest = getClient()
        return rest.client.delete(rest.newDeleteRequest(id)).result == DocWriteResponse.Result.DELETED
    }

    override fun batchDelete(docs: List<Document>): BatchDeleteAssetsResponse {
        if (docs.isEmpty()) { return BatchDeleteAssetsResponse() }

        val rsp = BatchDeleteAssetsResponse()
        val rest = getClient()
        val bulkRequest = BulkRequest()
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)

        docs.forEach { doc->
            if (doc.attrExists("system.hold") && doc.getAttr("system.hold", Boolean::class.java)) {
                rsp.onHoldAssetIds.add(doc.id)
            }
            else if (!hasPermission("write", doc)) {
                rsp.accessDeniedAssetIds.add(doc.id)
            }
            else {
                rsp.totalRequested+=1
                bulkRequest.add(rest.newDeleteRequest(doc.id))
            }
        }

        if (rsp.totalRequested == 0) {
            return rsp
        }

        val bulk = rest.client.bulk(bulkRequest)
        for (br in bulk.items) {
            when {
                br.isFailed -> {
                    logger.warnEvent(LogObject.ASSET, LogAction.BATCH_DELETE, br.failureMessage,
                            mapOf("assetId" to br.id, "index" to br.index))
                    rsp.errors[br.id] = br.failureMessage
                }
                else ->  {
                    val deleted =  br.getResponse<DeleteResponse>().result == DocWriteResponse.Result.DELETED
                    if (deleted) {
                        logger.event(LogObject.ASSET, LogAction.BATCH_DELETE, mapOf("assetId" to br.id, "index" to br.index))
                        rsp.deletedAssetIds.add(br.id)
                    }
                    else {
                        rsp.missingAssetIds.add(br.id)
                        logger.warnEvent(LogObject.ASSET, LogAction.BATCH_DELETE,  "Asset did not exist", mapOf("assetId" to br.id, "index" to br.index))
                    }
                }
            }
        }

        return rsp
    }


    override fun get(id: String): Document {
        return elastic.queryForObject(id, "asset", MAPPER)
    }

    override fun exists(path: Path): Boolean {
        val rest = getClient()
        val req = rest.newSearchBuilder()
        val source = req.source
        source.query(QueryBuilders.termQuery("source.path.raw", path.toString()))
        source.size(0)

        return rest.client.search(req.request).hits.totalHits > 0
    }

    override fun exists(id: String): Boolean {
        val rest = getClient()
        return rest.client.get(rest.newGetRequest(id)
                .fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE)).isExists
    }

    override fun get(path: Path): Document {
        val rest = getClient()
        val req = rest.newSearchBuilder()
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
        val rest = getClient()
        val req = rest.newSearchBuilder()
        rest.routeSearchRequest(req.request)

        req.request.apply {
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
                "if (ctx._source.system != null) {  "+
                "if (ctx._source.system.links != null) { " +
                "if (ctx._source.system.links[params.type] != null) { " +
                "ctx._source.system.links[params.type].removeIf(i-> i==params.id); }}}"

        private const val APPEND_LINK_SCRIPT =
                "if (ctx._source.system == null) { ctx._source.system = new HashMap(); } " +
                "if (ctx._source.system.links == null) { ctx._source.system.links = new HashMap(); }  " +
                "if (ctx._source.system.links[params.type] == null) { ctx._source.system.links[params.type] = new ArrayList(); }" +
                "ctx._source.system.links[params.type].add(params.id); "+
                "ctx._source.system.links[params.type] = new HashSet(ctx._source.system.links[params.type]);"

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
