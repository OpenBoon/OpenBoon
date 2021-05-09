package boonai.archivist.service

import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.DataSetUpdate
import boonai.archivist.domain.GenericBatchUpdateResponse
import boonai.archivist.domain.Model
import boonai.archivist.repository.DataSetDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.common.util.Json
import org.apache.lucene.search.join.ScoreMode
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DataSetService {

    fun createDataSet(spec: DataSetSpec): DataSet
    fun getDataSet(id: UUID): DataSet
    fun getDataSet(name: String): DataSet
    fun updateDataSet(dataSet: DataSet, update: DataSetUpdate)

    fun getLabelCounts(dataSet: DataSet): Map<String, Long>
    fun updateLabel(dataSet: DataSet, label: String, newLabel: String?): GenericBatchUpdateResponse

    fun getTestLabelSearch(): Map<String, Any>
}

@Service
@Transactional
class DataSetServiceImpl(
    val dataSetDao: DataSetDao,
    val indexRoutingService: IndexRoutingService,
    val assetSearchService: AssetSearchService
) : DataSetService {

    override fun createDataSet(spec: DataSetSpec): DataSet {

        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()

        val ds = DataSet(
            id,
            actor.projectId,
            spec.name,
            spec.type,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        dataSetDao.saveAndFlush(ds)
        return ds
    }

    override fun getDataSet(id: UUID): DataSet {
        return dataSetDao.getOneByProjectIdAndId(getProjectId(), id)
            ?: throw EmptyResultDataAccessException("The DataSet $id does not exist", 1)
    }

    override fun getDataSet(name: String): DataSet {
        return dataSetDao.getOneByProjectIdAndName(getProjectId(), name)
            ?: throw EmptyResultDataAccessException("The DataSet $name does not exist", 1)
    }

    override fun updateDataSet(dataSet: DataSet, update: DataSetUpdate) {
        dataSet.name = update.name
        dataSet.timeModified = System.currentTimeMillis()
        dataSet.actorModified = getZmlpActor().toString()
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun getLabelCounts(dataSet: DataSet): Map<String, Long> {
        val rest = indexRoutingService.getProjectRestClient()
        val modelIdFilter = QueryBuilders.termQuery("labels.dataSetId", dataSet.id.toString())
        val query = QueryBuilders.nestedQuery(
            "labels",
            modelIdFilter, ScoreMode.None
        )
        val agg = AggregationBuilders.nested("nested_labels", "labels")
            .subAggregation(
                AggregationBuilders.filter("filtered", modelIdFilter)
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
    override fun updateLabel(dataSet: DataSet, label: String, newLabel: String?): GenericBatchUpdateResponse {
        val rest = indexRoutingService.getProjectRestClient()

        val innerQuery = QueryBuilders.boolQuery()
        innerQuery.filter().add(QueryBuilders.termQuery("labels.dataSetId", dataSet.id.toString()))
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
                    "modelId" to dataSet.id.toString()
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
                    "modelId" to dataSet.id.toString()
                )
            )
        }

        val response: BulkByScrollResponse = rest.client.updateByQuery(req, RequestOptions.DEFAULT)
        return GenericBatchUpdateResponse(response.updated)
    }

    fun testLabelSearch(model: Model): Map<String, Any> {
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
                                        {"term": { "labels.modelId": "${model.id}" }},
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

    companion object {

        private val logger = LoggerFactory.getLogger(DataSetServiceImpl::class.java)

        /**
         * A painless script which renames a label.
         */
        private val RENAME_LABEL_SCRIPT =
            """
            for (int i = 0; i < ctx._source['labels'].length; ++i) {
               if (ctx._source['labels'][i]['label'] == params.oldLabel &&
                   ctx._source['labels'][i]['modelId'] == params.modelId) {
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
                   ctx._source['labels'][i]['modelId'] == params.modelId) {
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
        private val DELETE_MODEL_SCRIPT =
            """
            int index = -1;
            for (int i = 0; i < ctx._source['labels'].length; ++i) {
               if (ctx._source['labels'][i]['modelId'] == params.modelId) {
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
