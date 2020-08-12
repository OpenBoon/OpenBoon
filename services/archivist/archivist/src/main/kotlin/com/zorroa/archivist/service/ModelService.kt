package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.FileType
import com.zorroa.archivist.domain.GenericBatchUpdateResponse
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelApplyRequest
import com.zorroa.archivist.domain.ModelApplyResponse
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.StandardContainers
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.ModelDao
import com.zorroa.archivist.repository.ModelJdbcDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.util.Json
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.BulkByScrollResponse
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.BucketOrder
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ModelService {
    fun createModel(spec: ModelSpec): Model
    fun trainModel(model: Model, args: ModelTrainingArgs): Job
    fun getModel(id: UUID): Model
    fun find(filter: ModelFilter): KPagedList<Model>
    fun findOne(filter: ModelFilter): Model
    fun publishModel(model: Model, args: Map<String, Any>? = null): PipelineMod
    fun deployModel(model: Model, req: ModelApplyRequest): ModelApplyResponse
    fun getLabelCounts(model: Model): Map<String, Long>
    fun wrapSearchToExcludeTrainingSet(model: Model, search: Map<String, Any>): Map<String, Any>

    /**
     * Update a give label to a new label name.  If the new label name is null or
     * empty then remove the label.
     */
    fun updateLabel(model: Model, label: String, newLabel: String?): GenericBatchUpdateResponse
}

@Service
@Transactional
class ModelServiceImpl(
    val modelDao: ModelDao,
    val modelJdbcDao: ModelJdbcDao,
    val jobLaunchService: JobLaunchService,
    val jobService: JobService,
    val pipelineModService: PipelineModService,
    val indexRoutingService: IndexRoutingService,
    val assetSearchService: AssetSearchService
) : ModelService {

    override fun createModel(spec: ModelSpec): Model {
        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()

        val moduleName = (spec.moduleName ?: spec.type.moduleName ?: spec.name)

        if (moduleName.trim().isEmpty() || !moduleName.matches(modelNameRegex)) {
            throw IllegalArgumentException(
                "Model names must be alpha-numeric," +
                    " dashes,underscores, and spaces are allowed."
            )
        }

        val locator = ProjectFileLocator(
            ProjectStorageEntity.MODELS, id.toString(), "model", "model.zip"
        )

        val model = Model(
            id,
            getProjectId(),
            spec.type,
            spec.name,
            moduleName,
            locator.getFileId(),
            "Training model: ${spec.name} - [${spec.type.objective}]",
            false,
            spec.deploySearch, // VALIDATE THIS PARSES.
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        logger.event(
            LogObject.MODEL, LogAction.CREATE,
            mapOf(
                "modelId" to id,
                "modelType" to spec.type.name
            )
        )

        return modelDao.saveAndFlush(model)
    }

    @Transactional(readOnly = true)
    override fun getModel(id: UUID): Model {
        return modelDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun find(filter: ModelFilter): KPagedList<Model> {
        return modelJdbcDao.find(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: ModelFilter): Model {
        return modelJdbcDao.findOne(filter)
    }

    override fun trainModel(model: Model, args: ModelTrainingArgs): Job {

        val trainArgs = model.type.trainArgs.plus(
            mutableMapOf(
                "model_id" to model.id.toString(),
                "deploy" to args.deploy
            )
        ).plus(args.args ?: emptyMap())

        logger.info("Launching train job ${model.type.trainProcessor} $trainArgs")

        val processor = ProcessorRef(
            model.type.trainProcessor, "zmlp/plugins-train", trainArgs
        )

        modelJdbcDao.markAsReady(model.id, false)
        return jobLaunchService.launchTrainingJob(
            model.trainingJobName, processor, mapOf()
        )
    }

    override fun deployModel(model: Model, req: ModelApplyRequest): ModelApplyResponse {
        val name = "Deploying model: ${model.name}"
        var search = req.search ?: model.deploySearch

        if (!model.type.deployOnTrainingSet && !req.analyzeTrainingSet) {
            search = wrapSearchToExcludeTrainingSet(model, search)
        }

        val count = assetSearchService.count(search)
        if (count == 0L) {
            return ModelApplyResponse(0, null)
        }

        val repro = ReprocessAssetSearchRequest(
            search,
            listOf(model.moduleName),
            name = name,
            replace = true
        )

        return if (req.jobId == null) {
            val rsp = jobLaunchService.launchJob(repro)
            ModelApplyResponse(count, rsp.job)
        } else {
            val job = jobService.get(req.jobId, forClient = false)
            if (job.projectId != getProjectId()) {
                throw IllegalArgumentException("Unknown job Id ${job.id}")
            }
            val script = jobLaunchService.getReprocessTask(repro)
            jobService.createTask(job, script)
            ModelApplyResponse(count, job)
        }
    }

    override fun publishModel(model: Model, args: Map<String, Any>?): PipelineMod {
        val mod = pipelineModService.findByName(model.moduleName, false)
        val ops = buildModuleOps(model, args)

        if (mod != null) {
            // Set version number to change checksum
            val update = PipelineModUpdate(
                mod.name, mod.description, model.type.provider,
                mod.category, mod.type,
                listOf(FileType.Documents, FileType.Images),
                ops
            )
            pipelineModService.update(mod.id, update)
            return pipelineModService.get(mod.id)
        } else {
            val modspec = PipelineModSpec(
                model.moduleName,
                "Make predictions with your custom trained '${model.name}' model.",
                model.type.provider,
                Category.TRAINED,
                model.type.objective,
                listOf(FileType.Documents, FileType.Images),
                ops
            )

            modelJdbcDao.markAsReady(model.id, true)
            return pipelineModService.create(modspec)
        }
    }

    fun buildModuleOps(model: Model, args: Map<String, Any>?): List<ModOp> {
        val ops = mutableListOf<ModOp>()

        for (depend in model.type.dependencies) {
            val mod = pipelineModService.findByName(depend, true)
            ops.addAll(mod?.ops ?: emptyList())
        }

        // Add the dependency before.
        if (model.type.dependencies.isNotEmpty()) {
            ops.add(
                ModOp(
                    ModOpType.DEPEND,
                    model.type.dependencies
                )
            )
        }

        ops.add(
            ModOp(
                ModOpType.APPEND,
                listOf(
                    ProcessorRef(
                        model.type.classifyProcessor,
                        StandardContainers.ANALYSIS,
                        model.type.classifyArgs.plus(
                            mapOf(
                                "model_id" to model.id.toString(),
                                "version" to System.currentTimeMillis()
                            )
                        ).plus(args ?: emptyMap()),
                        module = model.name
                    )
                )
            )
        )

        return ops
    }

    override fun wrapSearchToExcludeTrainingSet(model: Model, search: Map<String, Any>): Map<String, Any> {
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
                                        {"term": { "labels.modelId": "${model.id}" }},
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

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun getLabelCounts(model: Model): Map<String, Long> {
        val rest = indexRoutingService.getProjectRestClient()
        val query = QueryBuilders.nestedQuery(
            "labels",
            QueryBuilders.termQuery("labels.modelId", model.id.toString()), ScoreMode.None
        )
        val agg = AggregationBuilders.nested("nested_labels", "labels")
            .subAggregation(
                AggregationBuilders.terms("labels")
                    .field("labels.label")
                    .size(1000)
                    .order(BucketOrder.key(true))
            )

        val req = rest.newSearchBuilder()
        req.source.query(query)
        req.source.aggregation(agg)
        req.source.size(0)
        req.source.fetchSource(false)

        val rsp = rest.client.search(req.request, RequestOptions.DEFAULT)
        val buckets = rsp.aggregations.get<Nested>("nested_labels").aggregations.get<Terms>("labels")

        // Use a LinkedHashMap to maintain sort on the labels.
        val result = LinkedHashMap<String, Long>()
        buckets.buckets.forEach {
            result[it.keyAsString] = it.docCount
        }
        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun updateLabel(model: Model, label: String, newLabel: String?): GenericBatchUpdateResponse {
        val rest = indexRoutingService.getProjectRestClient()

        val innerQuery = QueryBuilders.boolQuery()
        innerQuery.filter().add(QueryBuilders.termQuery("labels.modelId", model.id.toString()))
        innerQuery.filter().add(QueryBuilders.termQuery("labels.label", label))

        val query = QueryBuilders.nestedQuery(
            "labels", innerQuery, ScoreMode.None
        )

        val req = rest.newUpdateByQueryRequest()
        req.setQuery(query)
        req.isRefresh = true
        req.batchSize = 500
        req.isAbortOnVersionConflict = true

        req.script = if (newLabel.isNullOrEmpty()) {
            Script(
                ScriptType.INLINE,
                "painless",
                DELETE_LABEL_SCRIPT,
                mapOf(
                    "label" to label,
                    "modelId" to model.id.toString()
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
                    "modelId" to model.id.toString()
                )
            )
        }

        val response: BulkByScrollResponse = rest.client.updateByQuery(req, RequestOptions.DEFAULT)
        return GenericBatchUpdateResponse(response.updated)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ModelServiceImpl::class.java)

        private val modelNameRegex = Regex("^[a-z0-9_\\-\\s]{2,}$", RegexOption.IGNORE_CASE)

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
    }
}
