package com.zorroa.archivist.service

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceImportOptions
import com.zorroa.archivist.domain.FileTypes
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.ReprocessAssetSearchResponse
import com.zorroa.archivist.domain.StandardContainers
import com.zorroa.archivist.domain.ZpsScript
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException

interface JobLaunchService {
    /**
     * Launch an asset search reprocess request.
     */
    fun launchJob(req: ReprocessAssetSearchRequest): ReprocessAssetSearchResponse

    /**
     * Launch a process/reprocess of a DataSource.
     */
    fun launchJob(dataSource: DataSource, options: DataSourceImportOptions): Job

    /**
     * Launch a job with a generator.
     */
    fun launchJob(
        name: String,
        gen: ProcessorRef,
        pipeline: List<ProcessorRef>,
        settings: Map<String, Any>? = null,
        creds: Set<String>? = null
    ): Job

    /**
     * Launch a job with an array of assets.
     */
    fun launchJob(
        name: String,
        assets: List<String>,
        pipeline: List<ProcessorRef>,
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

    override fun launchJob(dataSource: DataSource, options: DataSourceImportOptions): Job {
        val gen = getGenerator(dataSource.uri)
        val mods = pipelineModService.getByIds(dataSource.modules)
        val modNames = mods.map { it.name }
        val name = "Applying modules: ${modNames.joinToString(",")} to ${dataSource.uri}"

        val script = ZpsScript(
            "Crawling files in '${dataSource.uri}'", listOf(gen), null,
            pipelineResolverService.resolveModular(mods)
        )

        script.setSettting("index", true)
        script.setSettting("fileTypes", dataSource.fileTypes)
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

        val name = "Applying modules: ${req.modules.joinToString(",")} to $count assets"
        val gen = ProcessorRef(
            "zmlp_core.core.generators.AssetSearchGenerator",
            StandardContainers.CORE,
            args = mapOf("search" to req.search)
        )

        val pipeline = pipelineResolverService.resolveModular(req.modules)
        val settings = mapOf(
            "index" to true,
            "batchSize" to clampBatchSize(req.batchSize),
            "fileTypes" to FileTypes.all
        )

        val job = launchJob(name, gen, pipeline, settings)
        return ReprocessAssetSearchResponse(job, count)
    }

    override fun launchJob(
        name: String,
        gen: ProcessorRef,
        pipeline: List<ProcessorRef>,
        settings: Map<String, Any>?,
        creds: Set<String>?
    ): Job {

        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }

        val script = ZpsScript(name, listOf(gen), null, pipeline, settings = mergedSettings)
        val spec = JobSpec(name, listOf(script), credentials = creds)
        return launchJob(spec)
    }

    override fun launchJob(
        name: String,
        assets: List<String>,
        pipeline: List<ProcessorRef>,
        settings: Map<String, Any>?,
        creds: Set<String>?
    ): Job {

        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }

        val script = ZpsScript(name, null, null, pipeline, settings = mergedSettings, assetIds = assets)
        val spec = JobSpec(name, listOf(script), credentials = creds)
        return launchJob(spec)
    }

    override fun launchTrainingJob(name: String, processor: ProcessorRef, settings: Map<String, Any>?): Job {
        val mergedSettings = getDefaultJobSettings()
        settings?.let { mergedSettings.putAll(it) }
        mergedSettings["index"] = false

        val script = ZpsScript(
            name, null, null,
            listOf(processor), settings = mergedSettings,
            assetIds = listOf("training")
        )

        val spec = JobSpec(name, listOf(script), replace = true, priority = JobPriority.Interactive)
        return launchJob(spec)
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
        const val maxBatchSize = 100

        fun clampBatchSize(batchSize: Int): Int {
            return batchSize.coerceAtLeast(minBatchSize).coerceAtMost(maxBatchSize)
        }
    }
}
