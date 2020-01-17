package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.repository.PipelineModDao
import com.zorroa.archivist.repository.ProjectCustomDao
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * --Attention--
 *
 * The Processor table is not being populated ATM so resolve a custom pipeline
 * just spits it back at you.
 *
 */
interface PipelineResolverService {

    fun resolve(pipeline: String?, modules: List<String>?): List<ProcessorRef>

    /**
     * Return a copy of the standard pipeline.
     */
    fun getStandardPipeline(): List<ProcessorRef>

    /**
     * Resolve the projects default pipeline.
     */
    fun resolve(): List<ProcessorRef>

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
    val projectCustomDao: ProjectCustomDao
) : PipelineResolverService {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var  pipelineService: PipelineService

    @Transactional(readOnly = true)
    override fun resolve(pipeline: String?, modules: List<String>?): List<ProcessorRef> {
        // Fetch the specified pipeline or default
        val pipe = if (pipeline != null) {
            pipelineService.get(pipeline)
        }
        else {
            val settings = projectCustomDao.getSettings(getProjectId())
            pipelineService.get(settings.defaultPipelineId)
        }

        if (pipe.mode == PipelineMode.MODULAR) {

            // Find the pipelines standard modules and convert
            // it into a mutable list.
            val mods = pipelineModService.getByIds(pipe.modules).toMutableList()

            modules?.let {
                // If the mod starts with -, we remove it from the pipeline
                // otherwise it's plussed on.
                val addOrRemoves = it?.map { mod-> !mod.startsWith("-") }
                val modMods =  pipelineModService.getByNames(it)

                for ((addOrRemove, modMod) in addOrRemoves.zip(modMods)) {
                    if (addOrRemove) {
                        mods.add(modMod)
                    }
                    else {
                        mods.remove(modMod)
                    }
                }
            }
            return resolveModular(mods)
        }
        else {
            return resolveCustom(pipe.processors)
        }
    }

    @Transactional(readOnly = true)
    override fun resolve(): List<ProcessorRef> {
        val settings = projectCustomDao.getSettings(getProjectId())
        return resolve(settings.defaultPipelineId)
    }

    @Transactional(readOnly = true)
    override fun resolve(id: UUID): List<ProcessorRef> {
        val pipeline = pipelineService.get(id)
        return if (pipeline.mode == PipelineMode.MODULAR) {
            val modules = pipelineModService.getByIds(pipeline.modules)
            resolveModular(modules)
        } else {
            resolveCustom(pipeline.processors)
        }
    }


    @Transactional(readOnly = true)
    override fun resolveModular(mods: List<PipelineMod>): List<ProcessorRef> {
        var currentPipeline = getStandardPipeline()

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
            val last = mutableListOf<ProcessorRef>()

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
                            ModOpType.LAST -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    last.addAll(Json.Mapper.convertValue<List<ProcessorRef>>(it))
                                }
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
            newPipeline.addAll(last)
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

            // No Op filter is a match.
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

    /**
     * TODO: allow replacement with bucket configuration file.
     */
    override fun getStandardPipeline(): List<ProcessorRef> {
        return listOf(
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
            ProcessorRef("PrependMarker", "none")
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResolverServiceImpl::class.java)
    }
}
