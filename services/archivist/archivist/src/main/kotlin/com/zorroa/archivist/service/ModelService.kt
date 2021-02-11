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
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.StandardContainers
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.ModelDao
import com.zorroa.archivist.repository.ModelJdbcDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.randomString
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.util.Json
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.toList

interface ModelService {
    fun createModel(spec: ModelSpec): Model
    fun trainModel(model: Model, args: ModelTrainingArgs): Job
    fun getModel(id: UUID): Model
    fun find(filter: ModelFilter): KPagedList<Model>
    fun findOne(filter: ModelFilter): Model
    fun publishModel(model: Model, args: Map<String, Any>? = null): PipelineMod
    fun setModelArgs(model: Model, args: Map<String, Any>): PipelineMod
    fun deployModel(model: Model, req: ModelApplyRequest): ModelApplyResponse
    fun deleteModel(model: Model)
    fun getLabelCounts(model: Model): Map<String, Long>
    fun wrapSearchToExcludeTrainingSet(model: Model, search: Map<String, Any>): Map<String, Any>

    /**
     * Update a give label to a new label name.  If the new label name is null or
     * empty then remove the label.
     */
    fun updateLabel(model: Model, label: String, newLabel: String?): GenericBatchUpdateResponse

    fun publishModelFileUpload(model: Model, inputStream: InputStream): PipelineMod

    fun validateTensorflowModel(path: Path)
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
    val assetSearchService: AssetSearchService,
    val fileStorageService: ProjectStorageService
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
            ?: throw EmptyResultDataAccessException("The model $id does not exist", 1)
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
            replace = true,
            includeStandard = false
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

    override fun setModelArgs(model: Model, args: Map<String, Any>): PipelineMod {
        val mod = pipelineModService.findByName(model.moduleName, false)
            ?: throw EmptyResultDataAccessException("Module not found, must be trained or published first", 1)

        val ops = buildModuleOps(model, args)
        val update = PipelineModUpdate(
            mod.name, mod.description, model.type.provider,
            mod.category, mod.type,
            listOf(FileType.Documents, FileType.Images),
            ops
        )
        pipelineModService.update(mod.id, update)
        return pipelineModService.get(mod.id)
    }

    override fun deleteModel(model: Model) {
        modelJdbcDao.delete(model)

        pipelineModService.findByName(model.moduleName, false)?.let {
            pipelineModService.delete(it.id)
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                fileStorageService.recursiveDelete(
                    ProjectDirLocator(ProjectStorageEntity.MODELS, model.id.toString())
                )
            } catch (e: Exception) {
                logger.error("Failed to delete files associated with model: ${model.id}")
            }
        }

        val rest = indexRoutingService.getProjectRestClient()
        val innerQuery = QueryBuilders.boolQuery()
        innerQuery.filter().add(QueryBuilders.termQuery("labels.modelId", model.id.toString()))

        val query = QueryBuilders.nestedQuery("labels", innerQuery, ScoreMode.None)
        val req = rest.newUpdateByQueryRequest()
        req.setQuery(query)
        req.isRefresh = false
        req.batchSize = 400
        req.isAbortOnVersionConflict = false
        req.script = Script(
            ScriptType.INLINE,
            "painless",
            DELETE_MODEL_SCRIPT,
            mapOf(
                "modelId" to model.id.toString()
            )
        )

        rest.client.updateByQueryAsync(
            req, RequestOptions.DEFAULT,
            object : ActionListener<BulkByScrollResponse> {

                override fun onFailure(e: java.lang.Exception?) {
                    logger.error("Failed to remove labels for model: ${model.id}", e)
                }

                override fun onResponse(response: BulkByScrollResponse?) {
                    logger.info("Removed ${response?.updated} labels from model: ${model.id}")
                }
            }
        )
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
        val modelIdFilter = QueryBuilders.termQuery("labels.modelId", model.id.toString())
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
        req.batchSize = 400
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

    override fun publishModelFileUpload(model: Model, inputStream: InputStream): PipelineMod {
        if (!model.type.uploadable) {
            throw IllegalArgumentException("The model type ${model.type} does not support uploads")
        }

        val tmpFile = Files.createTempFile(randomString(32), "model.zip")
        Files.copy(inputStream, tmpFile, StandardCopyOption.REPLACE_EXISTING)

        try {
            if (model.type == ModelType.TF2_IMAGE_CLASSIFIER) {
                validateTensorflowModel(tmpFile)
            }

            // Now store the file locally.
            val storage = ProjectStorageSpec(
                model.getModelStorageLocator(), mapOf(),
                FileInputStream(tmpFile.toFile()), Files.size(tmpFile)
            )
            val modelFile = fileStorageService.store(storage)

            // Now we can publish the model.
            return publishModel(model, mapOf("version" to System.currentTimeMillis()))
        } finally {
            Files.delete(tmpFile)
        }
    }

    override fun validateTensorflowModel(path: Path) {

        val zipFile = ZipFile(path.toFile())
        val files = zipFile.stream()
            .map(ZipEntry::getName)
            .map { it }.toList()

        if (!files.contains("labels.txt")) {
            throw IllegalArgumentException("The model zip must contain a labels.txt file")
        }

        /**
         * The valid files in a tensorflow zip file.
         */
        val validTensorflowFiles = listOf(
            "labels.txt",
            "saved_model.pb",
            "tfhub_module.pb",
            "assets/",
            "variables/",
            Regex("^variables/variables.data-[\\d]+-of-[\\d]+$"),
            "variables/variables.index"
        )

        files.forEach { fileName ->
            var matched = false

            for (pattern in validTensorflowFiles) {
                if (pattern is Regex) {
                    if (pattern.matches(fileName)) {
                        matched = true
                        break
                    }
                } else if (pattern is String) {
                    if (pattern.toString() == fileName) {
                        matched = true
                        break
                    }
                }
            }

            if (!matched) {
                throw IllegalArgumentException("'$fileName' is not an expected Tensorflow model file.")
            }
        }
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
