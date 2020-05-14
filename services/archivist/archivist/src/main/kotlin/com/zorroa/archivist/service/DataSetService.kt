package com.zorroa.archivist.service

import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetFilter
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.repository.DataSetDao
import com.zorroa.archivist.repository.DataSetJdbcDao
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DataSetService {

    /**
     * Create a new DataSet.
     */
    fun create(spec: DataSetSpec): DataSet

    /**
     * Get a DataSet by name.
     */
    fun get(name: String): DataSet

    /**
     * Get a DataSet by Id.
     */
    fun get(id: UUID): DataSet

    /**
     * Page,sort, and filter DataSets
     */
    fun find(filter: DataSetFilter): KPagedList<DataSet>

    /**
     * Find a single DataSet
     */
    fun findOne(filter: DataSetFilter): DataSet

    /**
     * Get a Map of all labels and label counts.
     */
    fun getLabelCounts(ds: DataSet): Map<String, Long>
}

@Service
@Transactional
class DataSetServiceImpl(
    val dataSetDao: DataSetDao,
    val dataSetJdbcDao: DataSetJdbcDao,
    val indexRoutingService: IndexRoutingService
) : DataSetService {

    override fun create(spec: DataSetSpec): DataSet {

        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor()

        if (!spec.name.matches(Regex("[a-z0-9\\-_]{2,32}", RegexOption.IGNORE_CASE))) {
            throw IllegalArgumentException(
                "DataSet names must be alpha numeric, dashes and underscores allowed")
        }

        val ts = DataSet(
            id,
            getProjectId(),
            spec.name,
            spec.type,
            time,
            time,
            actor.toString(),
            actor.toString()
        )
        dataSetDao.save(ts)
        logger.event(LogObject.DATASET, LogAction.CREATE, mapOf("dataSetId" to id))
        return ts
    }

    @Transactional(readOnly = true)
    override fun get(name: String): DataSet {
        return dataSetDao.getOneByProjectIdAndName(getProjectId(), name)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): DataSet {
        return dataSetDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun find(filter: DataSetFilter): KPagedList<DataSet> {
        return dataSetJdbcDao.find(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: DataSetFilter): DataSet {
        return dataSetJdbcDao.findOne(filter)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun getLabelCounts(ds: DataSet): Map<String, Long> {
        val rest = indexRoutingService.getProjectRestClient()
        val query = QueryBuilders.nestedQuery("labels",
            QueryBuilders.termQuery("labels.dataSetId", ds.id.toString()), ScoreMode.None)
        val agg = AggregationBuilders.nested("nested_labels", "labels")
            .subAggregation(AggregationBuilders.terms("labels").field("labels.label"))

        val req = rest.newSearchBuilder()
        req.source.query(query)
        req.source.aggregation(agg)
        req.source.size(0)
        req.source.fetchSource(false)

        val rsp = rest.client.search(req.request, RequestOptions.DEFAULT)
        val buckets = rsp.aggregations.get<Nested>("nested_labels").aggregations.get<Terms>("labels")
        return buckets.buckets.map { it.keyAsString to it.docCount }.toMap()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSetServiceImpl::class.java)
    }
}
