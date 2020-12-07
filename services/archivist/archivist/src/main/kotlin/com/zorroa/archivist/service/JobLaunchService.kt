package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceDelete
import com.zorroa.archivist.domain.DataSourceImportOptions
import com.zorroa.archivist.domain.FileExtResolver
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.ReprocessAssetSearchResponse
import com.zorroa.archivist.domain.ResolvedPipeline
import com.zorroa.archivist.domain.StandardContainers
import com.zorroa.archivist.domain.ZpsScript
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.stereotype.Component
import java.util.UUID

interface JobLaunchService {

    /**
     * Get a task for reprocessing assets.
     */
    fun getReprocessTask(req: ReprocessAssetSearchRequest, count: Long? = null): ZpsScript

    /**
     * Launch an asset search reprocess request.
     */
    fun launchJob(req: ReprocessAssetSearchRequest): ReprocessAssetSearchResponse

    /**
     * Launch a process/reprocess of a DataSource.
     */
    fun launchJob(dataSource: DataSource, options: DataSourceImportOptions): Job

    /**
     * Delete Datasource assets job.
     */
    fun launchJob(dataSource: DataSource, options: DataSourceDelete): Job

    /**
     * Launch a job with a generator.
     */
    fun launchJob(
        name: String,
        gen: ProcessorRef,
        pipeline: ResolvedPipeline,
        settings: Map<String, Any>? = null,
        creds: Set<String>? = null,
        dependOnJobIds: List<UUID>? = null
    ): Job

    /**
     * Launch a job with an array of assets.
     */
    fun launchJob(
        name: String,
        assets: List<String>,
        pipeline: ResolvedPipeline,
        settings: Map<String, Any>? = null,
        creds: Set<String>? = null
    ): Job

    /**
     * Launch a job with an array of assets.
     */
    fun launchTrainingJob(
        name: String,
        processor: ProcessorRef,
        settings: Map<String, Any>? = null
    ): Job
}

@Component
class JobLaunchServiceImpl(
    val pipelineModService: PipelineModService,
    val pipelineResolverService: PipelineResolverService,
    val pipelineService: PipelineService,
    val jobService: JobService,
    val assetSearchService: AssetSearchService
) : JobLaunchService {

    override fun launchJob(dataSource: DataSource, options: DataSourceDelete): Job {
        val name = "Deleting assets from: ${dataSource.name}"

        val script = ZpsScript(
            name = name,
            generate = listOf(
                ProcessorRef(
                    className = "zmlp_core.core.generators.DeleteBySearchGenerator",
                    image = StandardContainers.CORE,
                    args = mapOf("dataSourceId" to dataSource.id)
                )
            ),
            assets = null,
            execute = null
        )
        script.setSettting("fileTypes", FileExtResolver.resolve(dataSource.fileTypes))
        script.setSettting("batchSize", clampBatchSize(options.batchSize))

        val spec = JobSpec(
            name,
            listOf(script),
            credentials = options.credentials
        )
        return launchJob(spec)
    }

    override fun launchJob(dataSource: DataSource, options: DataSourceImportOptions): Job {
        val gen = getGenerator(dataSource.uri)
        val mods = pipelineModService.getByIds(dataSource.modules)
        val modNames = mods.map { it.name }
        val name = "Applying modules: ${modNames.joinToString(",")} to ${dataSource.uri}"

        val pipeline = pipelineResolverService.resolveModular(mods)

        val script = ZpsScript(
            "Crawling files in '${dataSource.uri}'", listOf(gen), null,
            execute = pipeline.execute, globalArgs = pipeline.globalArgs
        )

        script.setSettting("index", true)
        script.setSettting("fileTypes", FileExtResolver.resolve(dataSource.fileTypes))
        script.setSettting("batchSize", clampBatchSize(options.batchSize))

        val spec = JobSpec(
            name, listOf(script),
            dataSourceId = dataSource.id,
            credentials = dataSource.credentials.map { it.toString() }.toSet()
        )
        return launchJob(spec)
    }

    override fun launchJob(req: ReprocessAssetSearchRequest): ReprocessAssetSearchResponse {
        val count = assetSearchService.count(req.search)
        if (count == 0L) {
            throw DataRetrievalFailureException("Asset search did not return any assets")
        }
        val script = getReprocessTask(req, count)

        val spec = JobSpec(
            script.name,
            listOf(script),
            dependOnJobIds = req.dependOnJobIds,
            replace = req.replace
        )

        val job = launchJob(spec)
        return ReprocessAssetSearchResponse(job, count)
    }

    override fun launchJob(
        name: String,
        gen: ProcessorRef,
        pipeline: ResolvedPipeline,
        settings: Map<String, Any>?,
        creds: Set<String>?,
        dependOnJobIds: List<UUID>?
    ): Job {

        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }

        val script = ZpsScript(
            name, listOf(gen), null, pipeline.execute,
            settings = mergedSettings, globalArgs = pipeline.globalArgs
        )
        val spec = JobSpec(
            name,
            listOf(script),
            credentials = creds,
            dependOnJobIds = dependOnJobIds
        )
        return launchJob(spec)
    }

    override fun launchJob(
        name: String,
        assets: List<String>,
        pipeline: ResolvedPipeline,
        settings: Map<String, Any>?,
        creds: Set<String>?
    ): Job {

        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }

        val script = ZpsScript(
            name, null, null,
            pipeline.execute, settings = mergedSettings, assetIds = assets,
            globalArgs = pipeline.globalArgs
        )
        val spec = JobSpec(name, listOf(script), credentials = creds)
        return launchJob(spec)
    }

    override fun launchTrainingJob(name: String, processor: ProcessorRef, settings: Map<String, Any>?): Job {
        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }
        mergedSettings["index"] = false

        val script = ZpsScript(
            name, null, listOf(Asset("train")),
            listOf(processor), settings = mergedSettings
        )

        val spec = JobSpec(name, listOf(script), replace = true, priority = JobPriority.Interactive)
        return launchJob(spec)
    }

    override fun getReprocessTask(req: ReprocessAssetSearchRequest, count: Long?): ZpsScript {

        val assetCount = count ?: assetSearchService.count(req.search)
        if (assetCount == 0L) {
            throw DataRetrievalFailureException("Asset search did not return any assets")
        }

        val name = req.name
            ?: "Applying modules: ${req.modules.joinToString(",")} to $assetCount assets"
        val gen = ProcessorRef(
            "zmlp_core.core.generators.AssetSearchGenerator",
            StandardContainers.CORE,
            args = mapOf("search" to req.search)
        )

        val pipeline = pipelineResolverService.resolveModular(req.modules, req.includeStandard)
        val settings = mapOf(
            "index" to true,
            "batchSize" to clampBatchSize(req.batchSize),
            "fileTypes" to FileExtResolver.resolve(req.fileTypes)
        )

        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }

        return ZpsScript(
            name, listOf(gen), null, pipeline.execute,
            settings = mergedSettings, globalArgs = pipeline.globalArgs
        )
    }

    /**
     * Launch a [JobSpec] and return a [Job] suitable for client side use.
     */
    fun launchJob(spec: JobSpec): Job {
        val job = jobService.create(spec)
        return jobService.get(job.id, forClient = true)
    }

    /**
     * Return a map of default job settings.
     */
    fun getDefaultJobSettings(): MutableMap<String, Any?> {
        return mutableMapOf("batchSize" to defaultBatchSize)
    }

    /**
     * Get a suitable generator for the given url.
     */
    fun getGenerator(uri: String): ProcessorRef {

        val proc = when {
            uri.startsWith("gs://") -> "zmlp_core.core.generators.GcsBucketGenerator"
            uri.startsWith("s3://") -> "zmlp_core.core.generators.S3BucketGenerator"
            uri.startsWith("azure://") -> "zmlp_core.core.generators.AzureBucketGenerator"
            else -> throw IllegalArgumentException("Unknown URI type: $uri")
        }

        return ProcessorRef(
            proc,
            StandardContainers.CORE,
            args = mapOf("uri" to uri)
        )
    }

    companion object {
        /**
         * The default number of assets to add to a task.
         */
        const val defaultBatchSize = 25

        /**
         * Minimum batch size.
         */
        const val minBatchSize = 10

        /**
         * Maximum batch size.
         */
        const val maxBatchSize = 128

        fun clampBatchSize(batchSize: Int): Int {
            return batchSize.coerceAtLeast(minBatchSize).coerceAtMost(maxBatchSize)
        }
    }
}
