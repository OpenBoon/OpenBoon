package com.zorroa.archivist.elastic

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.Scroll
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.common.clients.SearchBuilder
import com.zorroa.common.util.Json
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.common.Strings
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.rest.action.search.RestSearchAction
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
import org.slf4j.LoggerFactory
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.dao.EmptyResultDataAccessException
import java.io.IOException
import java.io.OutputStream

class ElasticTemplate(private val indexRoutingService: IndexRoutingService) {

    fun <T> queryForObject(id: String, type: String?, mapper: SearchHitRowMapper<T>): T {
        val rest = indexRoutingService.getOrgRestClient()
        val req = rest.newGetRequest(id)
                .fetchSourceContext(FetchSourceContext.FETCH_SOURCE)

        val rsp = rest.client.get(req)
        if (!rsp.isExists) {
            throw EmptyResultDataAccessException(
                    "Expected 1 '$type' of id '$id'", 0)
        }
        try {
            return mapper.mapRow(SingleHit(rsp))
        } catch (e: Exception) {
            throw DataRetrievalFailureException("Failed to parse record, $e", e)
        }

    }

    fun <T> queryForObject(builder: SearchBuilder, mapper: SearchHitRowMapper<T>): T {
        val rest = indexRoutingService.getOrgRestClient()
        rest.routeSearchRequest(builder.request)

        val r = rest.client.search(builder.request)
        if (r.hits.totalHits == 0L) {
            throw EmptyResultDataAccessException("Expected 1 asset, got: 0", 0)
        }
        val hit = r.hits.getAt(0)
        try {
            return mapper.mapRow(SingleHit(hit))
        } catch (e: Exception) {
            throw DataRetrievalFailureException("Failed to parse record, $e", e)
        }

    }

    fun <T> scroll(id: String, timeout: String, mapper: SearchHitRowMapper<T>): PagedList<T> {
        val rest = indexRoutingService.getOrgRestClient()
        // already routed
        val req = SearchScrollRequest(id).scroll(timeout)
        val rsp = rest.client.searchScroll(req)

        val list = mutableListOf<T>()
        for (hit in rsp.hits.hits) {
            try {
                list.add(mapper.mapRow(SingleHit(hit)))
            } catch (e: Exception) {
                throw DataRetrievalFailureException("Failed to parse record, $e", e)
            }
        }

        val totalCount = rsp.hits.totalHits
        val result = PagedList(Pager().setTotalCount(totalCount), list)
        result.scroll = Scroll(rsp.scrollId)

        return result
    }

    fun <T> query(builder: SearchBuilder, mapper: SearchHitRowMapper<T>): List<T> {
        val rest = indexRoutingService.getOrgRestClient()
        rest.routeSearchRequest(builder.request)

        val r = rest.client.search(builder.request)
        val result = Lists.newArrayListWithCapacity<T>(r.hits.totalHits.toInt())

        for (hit in r.hits) {
            try {
                result.add(mapper.mapRow(SingleHit(hit)))
            } catch (e: Exception) {
                throw DataRetrievalFailureException("Failed to parse record, $e", e)
            }

        }

        return result
    }

    fun <T> page(builder: SearchBuilder, paging: Pager, mapper: SearchHitRowMapper<T>): PagedList<T> {
        val rest = indexRoutingService.getOrgRestClient()
        rest.routeSearchRequest(builder.request)
        builder.source.size(paging.size)
        builder.source.from(paging.from)

        val r = rest.client.search(builder.request)
        val list = Lists.newArrayListWithCapacity<T>(r.hits.hits.size)
        for (hit in r.hits.hits) {
            try {
                list.add(mapper.mapRow(SingleHit(hit)))
            } catch (e: Exception) {
                throw DataRetrievalFailureException("Failed to parse record, $e", e)
            }

        }

        paging.totalCount = r.hits.totalHits
        val result = PagedList(paging, list)
        result.scroll = Scroll(r.scrollId)


        if (r.aggregations != null) {
            try {
                val aggregations = r.aggregations
                val builder = XContentFactory.jsonBuilder().use { jb ->
                    jb.startObject()
                    aggregations.toXContent(jb, xContentParams)
                    jb.endObject()
                }
                var json = Strings.toString(builder).replace(REGEX_AGG_NAME_FIX, "\"$1\":")
                result.aggregations = Json.Mapper.readValue(json, Json.GENERIC_MAP)

            } catch (e: IOException) {
                logger.warn("Failed to deserialize aggregations.", e)
            }
        }

        return result
    }

    @Throws(IOException::class)
    fun page(builder: SearchBuilder, paging: Pager, out: OutputStream) {
        val rest = indexRoutingService.getOrgRestClient()
        rest.routeSearchRequest(builder.request)
        builder.source.size(paging.size)
        builder.source.from(paging.from)

        val r = rest.client.search(builder.request)

        XContentFactory.jsonBuilder(out).use { builder ->
            builder.startObject()
            builder.startArray("list")
            for (hit in r.hits.hits) {
                builder.startObject()
                builder.field("id", hit.id)
                builder.field("type", hit.type)
                builder.field("score", hit.score)
                builder.field("document", hit.sourceAsMap)
                builder.endObject()
            }
            builder.endArray()

            builder.startObject("page")
            builder.field("from", paging.from)
            builder.field("size", paging.size)
            builder.field("totalCount", r.hits.totalHits)
            builder.endObject()

            if (r.aggregations != null) {
                try {
                    builder.startObject("aggregations")
                    r.aggregations.toXContent(builder, xContentParams)
                    builder.endObject()
                } catch (e: Exception) {
                    logger.warn("Failed to deserialize aggregations.", e)
                }
            }
            builder.endObject()
        }
    }

    /**
     * Return the count for the given search.
     *
     * @param builder
     * @return
     */
    fun count(builder: SearchRequestBuilder): Long {
        builder.setSize(0)
        val r = builder.get()
        return r.hits.totalHits
    }

    /**
     * Index the given IndexRequestBuilder and return the document id.
     * @param builder
     * @return
     */
    fun index(builder: IndexRequestBuilder): String {
        return builder.get().id
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ElasticTemplate::class.java)

        /**
         * Replace the new fangled type/name agg.  The highLevelRestClient forces
         * this new naming convention.
         */
        private val REGEX_AGG_NAME_FIX = Regex("\"[\\w+.]+#([\\w+.]+)\":")

        /**
         * Instructs Agg system to not prefx agg names with the agg type.
         */
        private val xContentParams = ToXContent.MapParams(mapOf(RestSearchAction.TYPED_KEYS_PARAM to "false"))

    }
}
