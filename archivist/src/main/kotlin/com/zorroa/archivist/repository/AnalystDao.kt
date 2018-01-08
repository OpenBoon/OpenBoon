package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.elastic.AbstractElasticDao
import com.zorroa.common.elastic.SearchHitRowMapper
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.sort.SortOrder
import org.springframework.stereotype.Repository
import java.util.*

interface AnalystDao {

    fun getRunningTaskIds(): List<Int>

    fun register(spec: AnalystSpec): String

    fun setState(id: String, state: AnalystState)

    operator fun get(id: String): Analyst

    fun count(): Long

    fun delete(a: Analyst): Boolean

    fun getAll(paging: Pager): PagedList<Analyst>

    fun getExpired(limit: Int, duration: Long): List<Analyst>

    /**
     * Return a list of analysts that are in the UP state but are not updating
     * their data at regular intervals.
     *
     * @param limit
     * @param duration
     * @return
     */
    fun getUnresponsive(limit: Int, duration: Long): List<Analyst>

    fun getActive(paging: Pager): List<Analyst>

    fun getReady(paging: Pager): List<Analyst>
}

@Repository
open class AnalystDaoImpl : AbstractElasticDao(), AnalystDao {

    override fun getType(): String {
        return "analyst"
    }

    override fun getIndex(): String {
        return "analyst"
    }

    override fun register(spec: AnalystSpec): String {
        val doc = Json.serialize(spec)
        return elastic.index(client.prepareIndex(index, type, spec.id)
                .setSource(doc)
                .setOpType(IndexRequest.OpType.INDEX))
    }

    override fun setState(id: String, state: AnalystState) {
        client.prepareUpdate(index, type, id)
                .setDoc(ImmutableMap.of("state", state.ordinal))
                .setRefresh(true)
                .setRetryOnConflict(5)
                .get()
    }

    override fun get(id: String): Analyst {
        return if (id.contains(":")) {
            elastic.queryForObject(client.prepareSearch(index)
                    .setTypes(type)
                    .setQuery(QueryBuilders.termQuery("url", id)), MAPPER)
        } else {
            elastic.queryForObject(id, MAPPER)
        }
    }

    override fun count(): Long {
        return elastic.count(client.prepareSearch(index)
                .setTypes(type)
                .setQuery(QueryBuilders.matchAllQuery()))
    }

    override fun delete(a: Analyst): Boolean {
        return client.prepareDelete(index, type, a.id).get().isFound
    }

    override fun getRunningTaskIds(): List<Int> {
        val sr = client.prepareSearch(index)
                .setTypes(type)
                .setSize(0)
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(AggregationBuilders.terms("tasks").field("taskIds"))
                .get()

        val result = Lists.newArrayList<Int>()

        val tasks = sr.aggregations.get<Terms>("tasks")
        tasks.buckets.mapTo(result) { (it.key as Long).toInt() }
        Collections.sort(result)
        return result
    }

    override fun getAll(page: Pager): PagedList<Analyst> {
        return elastic.page(client.prepareSearch(index)
                .setTypes(type)
                .setSize(page.size)
                .setFrom(page.from)
                .setQuery(QueryBuilders.matchAllQuery()), page, MAPPER)
    }

    override fun getExpired(limit: Int, duration: Long): List<Analyst> {
        val query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("state", AnalystState.DOWN.ordinal))
                .must(QueryBuilders.rangeQuery("updatedTime").lt(System.currentTimeMillis() - duration))

        return elastic.query(client.prepareSearch(index)
                .setTypes(type)
                .setSize(limit)
                .setFrom(0)
                .setQuery(query), MAPPER)
    }

    override fun getUnresponsive(limit: Int, duration: Long): List<Analyst> {
        val query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal))
                .must(QueryBuilders.rangeQuery("updatedTime").lt(System.currentTimeMillis() - duration))

        return elastic.query(client.prepareSearch(index)
                .setTypes(type)
                .setSize(limit)
                .setFrom(0)
                .setQuery(query), MAPPER)
    }

    override fun getActive(paging: Pager): List<Analyst> {
        val query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal))

        return elastic.query(client.prepareSearch(index)
                .setTypes(type)
                .setSize(paging.size)
                .setFrom(paging.from)
                .addSort("loadAvg", SortOrder.ASC)
                .setQuery(query), MAPPER)
    }

    override fun getReady(paging: Pager): List<Analyst> {
        val query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal))
                .must(QueryBuilders.rangeQuery("remainingCapacity").gt(0))

        return elastic.query(client.prepareSearch(index)
                .setTypes(type)
                .setSize(paging.size)
                .setFrom(paging.from)
                .addSort("queueSize", SortOrder.ASC)
                .setQuery(query), MAPPER)
    }

    companion object {

        private val MAPPER = SearchHitRowMapper<Analyst> {
            hit -> Json.Mapper.convertValue(hit.source, Analyst::class.java).setId(hit.id) }
    }
}


