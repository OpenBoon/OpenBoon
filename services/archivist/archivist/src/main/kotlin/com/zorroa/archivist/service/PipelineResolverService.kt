package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.repository.PipelineModDao
import com.zorroa.archivist.util.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


/**
 * TODO: allow replacement with bucket configuration file.
 */
val STANDARD_PIPELINE = listOf(
    ProcessorRef("zmlp_core.core.processors.PreCacheSourceFileProcessor", "zmlp/plugins-core"),
    ProcessorRef("zmlp_core.image.importers.ImageImporter", "zmlp/plugins-core"),
    ProcessorRef("zmlp_core.office.importers.OfficeImporter", "zmlp/plugins-core"),
    ProcessorRef("zmlp_core.video.VideoImporter", "zmlp/plugins-core"),
    ProcessorRef(
        "zmlp_core.core.processors.AssertAttributesProcessor", "zmlp/plugins-core",
        mapOf("attrs" to listOf("media.type"))
    ),
    ProcessorRef("zmlp_core.proxy.ImageProxyProcessor", "zmlp/plugins-core"),
    ProcessorRef("zmlp_core.proxy.VideoProxyProcessor", "zmlp/plugins-core"),
    ProcessorRef("zmlp_analysis.mxnet.processors.ResNetSimilarityProcessor", "zmlp/plugins-analysis"),
    ProcessorRef("zmlp_analysis.mxnet.processors.ResNetClassifyProcessor", "zmlp/plugins-analysis"),
    ProcessorRef("zmlp_analysis.detect.ZmlpObjectDetectionProcessor", "zmlp/plugins-analysis")
)

/**
 * --Attention--
 *
 * The Processor table is not being populated ATM so resolve a custom pipeline
 * just spits it back at you.
 *
 */
interface PipelineResolverService {
    /**
     * Resolve the given [Pipeline] ID into a list of [ProcessorRef]
     */
    fun resolve(id: UUID): List<ProcessorRef>

    /**
     * Resolve a list of [Pipeline]] Mods into a list of [ProcessorRef]
     */
    fun resolveModular(mods: List<PipelineMod>): List<ProcessorRef>

    /**
     * Resolve a lis of [ProcessorRef] into a new list of [ProcessorRef]
     */
    fun resolveCustom(refs: List<ProcessorRef>?): MutableList<ProcessorRef>
}

@Service
@Transactional
class PipelineResolverServiceImpl(
    val pipelineService: PipelineService,
    val pipelineModDao: PipelineModDao
) : PipelineResolverService {

    @Transactional(readOnly = true)
    override fun resolve(id: UUID): List<ProcessorRef> {
        val pipeline = pipelineService.get(id)
        return if (pipeline.mode == PipelineMode.MODULAR) {
            val modules = pipelineModDao.findByIdIn(pipeline.modules)
            resolveModular(modules)
        } else {
            resolveCustom(pipeline.processors)
        }
    }

    @Transactional(readOnly = true)
    override fun resolveModular(mods: List<PipelineMod>): List<ProcessorRef> {

        // Make a copy first, otherwise we'll corrupt other pipelines.
        var currentPipeline = mutableListOf<ProcessorRef>()
        STANDARD_PIPELINE.mapTo(currentPipeline) {
            ProcessorRef(it.className, it.image, it.args?.toMap())
        }

        // Add a marker for the prepend point. This gets removed later
        currentPipeline.add(ProcessorRef("PrependMarker", "none"))

        for (module in mods) {

            /**
             * Builds a parallel array of opts for each ProcessorRef in the pipeline.
             */
            val matchingOps: List<List<ModOp>> = currentPipeline.map { ref ->
                getMatchingOps(ref, module)
            }

            val newPipeline = mutableListOf<ProcessorRef>()
            val append = mutableListOf<ProcessorRef>()
            val prepend = mutableListOf<ProcessorRef>()

            currentPipeline.zip(matchingOps).forEach { (ref, ops) ->
                //  If no ops matched, the processor just passes through to this
                // new pipeline.
                if (ops.isNullOrEmpty()) {
                    newPipeline.add(ref)
                } else {
                    // If some ops matched, they get applied top the pipeline.
                    for (op in ops) {
                        when (op.type) {
                            ModOpType.ADD_AFTER -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    newPipeline.addAll(Json.Mapper.convertValue<List<ProcessorRef>>(it))
                                }
                            }
                            ModOpType.ADD_BEFORE -> {
                                op.apply?.let {
                                    newPipeline.addAll(Json.Mapper.convertValue<List<ProcessorRef>>(it))
                                }
                                newPipeline.add(ref)
                            }
                            ModOpType.APPEND -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    append.addAll(Json.Mapper.convertValue<List<ProcessorRef>>(it))
                                }
                            }
                            ModOpType.PREPEND -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    prepend.addAll(Json.Mapper.convertValue<List<ProcessorRef>>(it))
                                }
                            }
                            ModOpType.REMOVE -> {
                                // just don't do anything.
                            }
                            ModOpType.REPLACE -> {
                                op.apply?.let {
                                    newPipeline.addAll(Json.Mapper.convertValue<List<ProcessorRef>>(it))
                                }
                            }
                            ModOpType.SET_ARGS -> {
                                op.apply?.let {
                                    val args = (ref.args?.toMutableMap()) ?: mutableMapOf()
                                    args.putAll(Json.Mapper.convertValue<Map<String, Any>>(op.apply))
                                    ref.args = args
                                }
                                newPipeline.add(ref)
                            }
                        }
                    }
                }
            }

            val prependMarker = newPipeline.indexOfFirst { it.className == "PrependMarker" }
            newPipeline.addAll(prependMarker, prepend)
            newPipeline.addAll(append)
            currentPipeline = newPipeline
        }

        return currentPipeline.filterNot { it.className == "PrependMarker" }
    }

    @Transactional(readOnly = true)
    override fun resolveCustom(refs: List<ProcessorRef>?): MutableList<ProcessorRef> {
        val result = mutableListOf<ProcessorRef>()

        refs?.forEach { ref ->
            result.add(ref)
            ref.execute?.let {
                ref.execute = resolveCustom(it)
            }
        }
        return result
    }

    /**
     * Return a list of all matching [ModOp]s from the given [PipelineMod]
     */
    fun getMatchingOps(ref: ProcessorRef, mod: PipelineMod): List<ModOp> {
        val result = mutableListOf<ModOp>()
        for (op in mod.ops) {
            if (op.applyCount >= op.maxApplyCount) {
                continue
            }

            val matched = op.filter?.let {
                val processor = it.processor ?: ""
                when (it.type) {
                    OpFilterType.REGEX -> Regex(processor).matches(ref.className)
                    OpFilterType.SUBSTR -> processor in ref.className
                    OpFilterType.EQUAL -> processor == ref.className
                    OpFilterType.NOT_REGEX -> Regex(processor).matches(ref.className)
                    OpFilterType.NOT_SUBSTR -> processor in ref.className
                }
            } ?: true
            if (matched) {
                op.applyCount += 1
                result.add(op)
            }
        }

        return result
    }
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResolverServiceImpl::class.java)
    }
}
