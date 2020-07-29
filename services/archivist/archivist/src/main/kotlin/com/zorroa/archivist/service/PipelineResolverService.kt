package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.archivist.repository.ProjectCustomDao
import com.zorroa.zmlp.util.Json
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
    fun getStandardPipeline(trimPrependMarker: Boolean = false): List<ProcessorRef>

    /**
     * Resolve the projects default pipeline.
     */
    fun resolve(): List<ProcessorRef>

    /**
     * Resolve the given Pipeline Mods ID into a list of [ProcessorRef]
     */
    fun resolve(id: UUID): List<ProcessorRef>

    /**
     * Resolve a Pipeline object.
     */
    fun resolve(pipeline: Pipeline): List<ProcessorRef>

    /**
     * Resolve a list of Pipeline Mods into a list of [ProcessorRef]
     */
    fun resolveModular(mods: List<PipelineMod>): List<ProcessorRef>

    /**
     * Resolve a list of [ProcessorRef] into a new list of [ProcessorRef]
     */
    fun resolveCustom(refs: List<ProcessorRef>?): MutableList<ProcessorRef>

    /**
     * Resolve a list of module names or ids into a new list of [ProcessorRef]
     */
    fun resolveModular(mods: Collection<String>?): List<ProcessorRef>
}

@Service
@Transactional
class PipelineResolverServiceImpl(
    val projectCustomDao: ProjectCustomDao,
    val pipelineDao: PipelineDao
) : PipelineResolverService {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Transactional(readOnly = true)
    override fun resolve(pipeline: String?, modules: List<String>?): List<ProcessorRef> {
        // Fetch the specified pipeline or default
        val pipe = if (pipeline != null) {
            pipelineService.get(pipeline)
        } else {
            pipelineDao.getDefault()
        }

        if (pipe.mode == PipelineMode.MODULAR) {

            // Find the pipelines standard modules and convert
            // it into a mutable list.
            val mods = pipelineModService.getByIds(pipe.modules).toMutableList()

            modules?.let {
                // If the mod starts with -, we remove it from the pipeline
                // otherwise it's plussed on.
                val addOrRemoves = it?.map { mod -> !mod.startsWith("-") }
                val modMods = pipelineModService.getByNames(it)

                for ((addOrRemove, modMod) in addOrRemoves.zip(modMods)) {
                    if (addOrRemove) {
                        mods.add(modMod)
                    } else {
                        mods.remove(modMod)
                    }
                }
            }
            return resolveModular(mods)
        } else {
            return resolveCustom(pipe.processors)
        }
    }

    @Transactional(readOnly = true)
    override fun resolve(): List<ProcessorRef> {
        val pipeline = pipelineDao.getDefault()
        return resolve(pipeline)
    }

    @Transactional(readOnly = true)
    override fun resolve(pipeline: Pipeline): List<ProcessorRef> {
        return if (pipeline.mode == PipelineMode.MODULAR) {
            val modules = pipelineModService.getByIds(pipeline.modules)
            resolveModular(modules)
        } else {
            resolveCustom(pipeline.processors)
        }
    }

    @Transactional(readOnly = true)
    override fun resolve(id: UUID): List<ProcessorRef> {
        return resolve(pipelineService.get(id))
    }

    @Transactional(readOnly = true)
    override fun resolveModular(mods: Collection<String>?): List<ProcessorRef> {
        return resolveModular(pipelineModService.getByNames(mods ?: listOf()))
    }

    @Transactional(readOnly = true)
    override fun resolveModular(mods: List<PipelineMod>): List<ProcessorRef> {

        /**
         * The current pipeline.  This is re-resolved after every pipeline module, which
         * means that ModOps in the same module cannot see modifications made by
         * previous ModOps.
         */
        var currentPipeline = getStandardPipeline()

        for (module in mods) {

            /**
             * Builds a parallel array of opts for each ProcessorRef in the pipeline.
             */
            val matchingOps: List<List<ModOp>> = currentPipeline.map { ref ->
                getMatchingOps(ref, module)
            }

            /**
             * The newPipeline becomes the currentPipeline after each mod iteration.
             */
            val newPipeline = mutableListOf<ProcessorRef>()

            /**
             * The append list contains newly appended processors.
             */
            val append = mutableListOf<ProcessorRef>()

            /**
             * The prepend list contains newly prepended processors which get put at
             * the prepend marker.
             */
            val prepend = mutableListOf<ProcessorRef>()

            /**
             * The last list contains newly appended  processors that must be
             * last or close to last. Usually these don't modify assets, but
             * emit them somewhere.
             */
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
                                    newPipeline.addAll(parsePipelineFragment(module.name, it))
                                }
                            }
                            ModOpType.ADD_BEFORE -> {
                                op.apply?.let {
                                    newPipeline.addAll(parsePipelineFragment(module.name, it))
                                }
                                newPipeline.add(ref)
                            }
                            ModOpType.LAST -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    last.addAll(parsePipelineFragment(module.name, it))
                                }
                            }
                            ModOpType.APPEND -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    append.addAll(parsePipelineFragment(module.name, it))
                                }
                            }
                            ModOpType.APPEND_MERGE -> {
                                newPipeline.add(ref)
                                val names = (
                                    currentPipeline.map { it.className } +
                                        newPipeline.map { it.className } +
                                        append.map { it.className }
                                    )
                                // Iterate procs in the fragment and add ones that don't exist,
                                // and merge args for the ones that do.
                                val frag = parsePipelineFragment(module.name, op.apply)
                                for (proc in frag) {
                                    if (proc.className !in names) {
                                        append.add(proc)
                                    } else {
                                        val existing = currentPipeline.find { it == proc }
                                            ?: newPipeline.find { it == proc }
                                            ?: append.find { it == proc }
                                        existing?.let { p ->
                                            p.args = (p.args ?: mapOf()) + (proc.args ?: mapOf())
                                        }
                                    }
                                }
                            }
                            ModOpType.PREPEND -> {
                                newPipeline.add(ref)
                                op.apply?.let {
                                    prepend.addAll(parsePipelineFragment(module.name, it))
                                }
                            }
                            ModOpType.REMOVE -> {
                                // just don't do anything.
                            }
                            ModOpType.REPLACE -> {
                                op.apply?.let {
                                    newPipeline.addAll(parsePipelineFragment(module.name, it))
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

    fun parsePipelineFragment(module: String, frag: Any?): List<ProcessorRef> {
        if (frag == null) {
            return emptyList()
        }
        val result = Json.Mapper.convertValue<List<ProcessorRef>>(frag)
        // Sets the module name
        result.forEach { it.module = module }
        return result
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
    override fun getStandardPipeline(trimPrependMarker: Boolean): List<ProcessorRef> {
        return listOf(
            ProcessorRef("zmlp_core.core.PreCacheSourceFileProcessor", "zmlp/plugins-core"),
            ProcessorRef("zmlp_core.core.FileImportProcessor", "zmlp/plugins-core"),
            ProcessorRef("zmlp_core.proxy.ImageProxyProcessor", "zmlp/plugins-core"),
            ProcessorRef("zmlp_core.proxy.VideoProxyProcessor", "zmlp/plugins-core"),
            ProcessorRef("zmlp_analysis.zvi.ZviSimilarityProcessor", "zmlp/plugins-analysis"),
            ProcessorRef("PrependMarker", "none")
        ).dropLastWhile { trimPrependMarker }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResolverServiceImpl::class.java)
    }
}
