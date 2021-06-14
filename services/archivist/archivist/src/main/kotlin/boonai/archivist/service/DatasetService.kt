package boonai.archivist.service

import boonai.archivist.clients.EsRestClient
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.DatasetFilter
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetUpdate
import boonai.archivist.domain.GenericBatchUpdateResponse
import boonai.archivist.domain.LabelScope
import boonai.archivist.domain.LabelSet
import boonai.archivist.repository.DatasetDao
import boonai.archivist.repository.DatasetJdbcDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.CoroutineAuthentication
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.common.util.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.ActionListener
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.BulkByScrollResponse
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.BucketOrder
import org.elasticsearch.search.aggregations.bucket.filter.Filter
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DatasetService {

    fun createDataset(spec: DatasetSpec): Dataset
    fun getDataset(id: UUID): Dataset
    fun getDataset(name: String): Dataset
    fun updateDataset(dataset: Dataset, update: DatasetUpdate)
    fun deleteDataset(dataset: Dataset)
    fun findOne(filter: DatasetFilter): Dataset
    fun find(filter: DatasetFilter): KPagedList<Dataset>
    fun getLabelCounts(dataset: Dataset): Map<String, Long>
    fun updateLabel(dataset: Dataset, label: String, newLabel: String?): GenericBatchUpdateResponse
    fun getLabelCountsV4(dataSst: Dataset): Map<String, Map<String, Long>>
    fun buildTestLabelSearch(labelSet: LabelSet): Map<String, Any>
    fun wrapSearchToExcludeTrainingSet(labelSet: LabelSet, search: Map<String, Any>): Map<String, Any>
}

@Service
@Transactional
class DatasetServiceImpl(
    val datasetDao: DatasetDao,
    val datasetJdbcDao: DatasetJdbcDao,
    val indexRoutingService: IndexRoutingService,
    val assetSearchService: AssetSearchService
) : DatasetService {

    override fun createDataset(spec: DatasetSpec): Dataset {

        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()

        val ds = Dataset(
            id,
            actor.projectId,
            spec.name,
            spec.type,
            spec.description,
            0,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        datasetDao.saveAndFlush(ds)
        return ds
    }

    @Transactional(readOnly = true)
    override fun getDataset(id: UUID): Dataset {
        return datasetDao.getOneByProjectIdAndId(getProjectId(), id)
            ?: throw EmptyResultDataAccessException("The Dataset $id does not exist", 1)
    }

    @Transactional(readOnly = true)
    override fun getDataset(name: String): Dataset {
        return datasetDao.getOneByProjectIdAndName(getProjectId(), name)
            ?: throw EmptyResultDataAccessException("The Dataset $name does not exist", 1)
    }

    @Transactional(readOnly = true)
    override fun find(filter: DatasetFilter): KPagedList<Dataset> {
        return datasetJdbcDao.find(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: DatasetFilter): Dataset {
        return datasetJdbcDao.findOne(filter)
    }

    override fun deleteDataset(dataset: Dataset) {
        datasetDao.delete(dataset)

        val rest = indexRoutingService.getProjectRestClient()
        GlobalScope.launch(Dispatchers.IO + CoroutineAuthentication(SecurityContextHolder.getContext())) {
            removeAllLabels(rest, dataset)
        }
    }

    override fun updateDataset(dataset: Dataset, update: DatasetUpdate) {
        // Note you can't change type because different types have
        // different label requirements.
        dataset.name = update.name
        dataset.timeModified = System.currentTimeMillis()
        dataset.actorModified = getZmlpActor().toString()
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun getLabelCounts(dataset: Dataset): Map<String, Long> {
        val rest = indexRoutingService.getProjectRestClient()
        val dsFilter = QueryBuilders.termQuery("labels.datasetId", dataset.id.toString())

        val query = QueryBuilders.nestedQuery(
            "labels",
            dsFilter, ScoreMode.None
        )
        val agg = AggregationBuilders.nested("nested_labels", "labels")
            .subAggregation(
                AggregationBuilders.filter("filtered", dsFilter)
                    .subAggregation(
                        AggregationBuilders.terms("labels")
                            .field("labels.label")
                            .size(1000)
                            .order(BucketOrder.key(true))
                    )
            )

        val req = rest.newSearchBuilder()
        req.source.query(query)
        req.source.aggregation(agg)
        req.source.size(0)
        req.source.fetchSource(false)

        val rsp = rest.client.search(req.request, RequestOptions.DEFAULT)
        val buckets = rsp.aggregations.get<Nested>("nested_labels")
            .aggregations.get<Filter>("filtered")
            .aggregations.get<Terms>("labels")

        // Use a LinkedHashMap to maintain sort on the labels.
        val result = LinkedHashMap<String, Long>()
        buckets.buckets.forEach {
            result[it.keyAsString] = it.docCount
        }
        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun getLabelCountsV4(dataSet: Dataset): Map<String, Map<String, Long>> {
        val rest = indexRoutingService.getProjectRestClient()
        val dsFilter = QueryBuilders.termQuery("labels.dataSetId", dataSet.id.toString())
        val query = QueryBuilders.nestedQuery(
            "labels",
            dsFilter, ScoreMode.None
        )
        val agg = AggregationBuilders.nested("nested_labels", "labels")
            .subAggregation(
                AggregationBuilders.filter("filtered", dsFilter)
                    .subAggregation(
                        AggregationBuilders.terms("labels")
                            .field("labels.label")
                            .size(1000)
                            .order(
                                BucketOrder.key(true)
                            ).subAggregation(
                                AggregationBuilders.terms("scope")
                                    .field("labels.scope")
                            )
                    )
            )

        val req = rest.newSearchBuilder()
        req.source.query(query)
        req.source.aggregation(agg)
        req.source.size(0)
        req.source.fetchSource(false)

        val rsp = rest.client.search(req.request, RequestOptions.DEFAULT)
        val buckets = rsp.aggregations.get<Nested>("nested_labels")
            .aggregations.get<Filter>("filtered")
            .aggregations.get<Terms>("labels")

        // Use a LinkedHashMap to maintain sort on the labels.
        val result = LinkedHashMap<String, MutableMap<String, Long>>()
        buckets.buckets.forEach {
            // Zero out possible keys.
            result[it.keyAsString] = mutableMapOf(LabelScope.TEST.name to 0L, LabelScope.TRAIN.name to 0L)
            for (scope in it.aggregations.get<Terms>("scope").buckets) {
                result[it.keyAsString]?.set(scope.keyAsString, scope.docCount)
            }
        }
        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun updateLabel(dataset: Dataset, label: String, newLabel: String?): GenericBatchUpdateResponse {
        val rest = indexRoutingService.getProjectRestClient()

        val innerQuery = QueryBuilders.boolQuery()
        innerQuery.filter().add(QueryBuilders.termQuery("labels.datasetId", dataset.id.toString()))
        innerQuery.filter().add(QueryBuilders.termQuery("labels.label", label))

        val query = QueryBuilders.nestedQuery(
            "labels", innerQuery, ScoreMode.None
        )

        val req = rest.newUpdateByQueryRequest()
        req.setQuery(query)
        req.isRefresh = true
        req.batchSize = 400
        req.isAbortOnVersionConflict = true

        req.script = if (newLabel.isNullOrEmpty()) {
            Script(
                ScriptType.INLINE,
                "painless",
                DELETE_LABEL_SCRIPT,
                mapOf(
                    "label" to label,
                    "datasetId" to dataset.id.toString()
                )
            )
        } else {
            Script(
                ScriptType.INLINE,
                "painless",
                RENAME_LABEL_SCRIPT,
                mapOf(
                    "oldLabel" to label,
                    "newLabel" to newLabel,
                    "datasetId" to dataset.id.toString()
                )
            )
        }

        val response: BulkByScrollResponse = rest.client.updateByQuery(req, RequestOptions.DEFAULT)
        return GenericBatchUpdateResponse(response.updated)
    }

    override fun buildTestLabelSearch(labelSet: LabelSet): Map<String, Any> {
        val wrapper =
            """
            {
            	"bool": {
                    "filter": {
                        "nested" : {
                            "path": "labels",
                            "query" : {
                                "bool": {
                                    "filter": [
                                        {"term": { "labels.datasetId": "${labelSet.datasetId()}" }},
                                        {"term": { "labels.scope": "TEST"}}
                                    ]
                                }
                            }
                        }
                    }
            	}
            }
        """.trimIndent()
        return mapOf("query" to Json.Mapper.readValue(wrapper, Json.GENERIC_MAP))
    }

    override fun wrapSearchToExcludeTrainingSet(lableSet: LabelSet, search: Map<String, Any>): Map<String, Any> {
        val emptySearch = mapOf("match_all" to mapOf<String, Any>())
        val query = Json.serializeToString(search.getOrDefault("query", emptySearch))

        val wrapper =
            """
            {
                "bool": {
                    "must": $query,
                    "must_not": {
                        "nested" : {
                            "path": "labels",
                            "query" : {
                                "bool": {
                                    "filter": [
                                        {"term": { "labels.datasetId": "${lableSet.datasetId()}" }},
                                        {"term": { "labels.scope": "TRAIN"}}
                                    ]
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent()
        val result = search.toMutableMap()
        result["query"] = Json.Mapper.readValue(wrapper, Json.GENERIC_MAP)
        return result
    }

    fun removeAllLabels(rest: EsRestClient, dataset: Dataset) {

        logger.info("Removing all labels for ${dataset.name}")
        val innerQuery = QueryBuilders.boolQuery()
        innerQuery.filter().add(QueryBuilders.termQuery("labels.datasetId", dataset.id.toString()))

        val query = QueryBuilders.nestedQuery("labels", innerQuery, ScoreMode.None)
        val req = rest.newUpdateByQueryRequest()
        req.setQuery(query)
        req.isRefresh = false
        req.batchSize = 400
        req.isAbortOnVersionConflict = false
        req.script = Script(
            ScriptType.INLINE,
            "painless",
            DELETE_DS_SCRIPT,
            mapOf(
                "datasetId" to dataset.id.toString()
            )
        )

        rest.client.updateByQueryAsync(
            req, RequestOptions.DEFAULT,
            object : ActionListener<BulkByScrollResponse> {

                override fun onFailure(e: java.lang.Exception?) {
                    logger.error("Failed to remove labels for Dataset: ${dataset.id}", e)
                }

                override fun onResponse(response: BulkByScrollResponse?) {
                    logger.info("Removed ${response?.updated} labels from Dataset: ${dataset.id}")
                }
            }
        )
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DatasetServiceImpl::class.java)

        /**
         * A painless script which renames a label.
         */
        private val RENAME_LABEL_SCRIPT =
            """
            for (int i = 0; i < ctx._source['labels'].length; ++i) {
               if (ctx._source['labels'][i]['label'] == params.oldLabel &&
                   ctx._source['labels'][i]['datasetId'] == params.datasetId) {
                       ctx._source['labels'][i]['label'] = params.newLabel;
                       break;
               }
            }
            """.trimIndent()

        /**
         * A painless script which renames a label.
         */
        private val DELETE_LABEL_SCRIPT =
            """
            int index = -1;
            for (int i = 0; i < ctx._source['labels'].length; ++i) {
               if (ctx._source['labels'][i]['label'] == params.label &&
                   ctx._source['labels'][i]['datasetId'] == params.datasetId) {
                   index = i;
                   break;
               }
            }
            if (index > -1) {
               ctx._source['labels'].remove(index)
            }
            """.trimIndent()

        /**
         * A painless script which renames a label.
         */
        private val DELETE_DS_SCRIPT =
            """
            int index = -1;
            for (int i = 0; i < ctx._source['labels'].length; ++i) {
               if (ctx._source['labels'][i]['dataSetId'] == params.dataSetId) {
                   index = i;
                   break;
               }
            }
            if (index > -1) {
               ctx._source['labels'].remove(index)
            }
            """.trimIndent()
    }
}
