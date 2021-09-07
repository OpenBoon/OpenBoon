package boonai.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import boonai.archivist.domain.ModOp
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.OpFilterType
import boonai.archivist.domain.Pipeline
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineMode
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.ResolvedPipeline
import boonai.archivist.repository.PipelineDao
import boonai.common.util.Json
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

    fun resolve(pipeline: String?, modules: List<String>?): ResolvedPipeline

    /**
     * Return a copy of the standard pipeline.
     */
    fun getStandardPipeline(trimPrependMarker: Boolean = false): List<ProcessorRef>

    /**
     * Resolve the projects default pipeline.
     */
    fun resolve(): ResolvedPipeline

    /**
     * Resolve the given Pipeline Mods ID into a list of [ProcessorRef]
     */
    fun resolve(id: UUID): ResolvedPipeline

    /**
     * Resolve a Pipeline object.
     */
    fun resolve(pipeline: Pipeline): ResolvedPipeline

    /**
     * Resolve a list of Pipeline Mods into a list of [ProcessorRef]
     */
    fun resolveModular(mods: List<PipelineMod>, includeStandard: Boolean = true): ResolvedPipeline

    /**
     * Resolve a list of [ProcessorRef] into a new list of [ProcessorRef]
     */
    fun resolveCustom(refs: List<ProcessorRef>?): ResolvedPipeline

    /**
     * Resolve a list of module names or ids into a new list of [ProcessorRef]
     */
    fun resolveModular(mods: Collection<String>?, includeStandard: Boolean = true): ResolvedPipeline

    /**
     * Resolve a list of processors into a new list of processors.
     */
    fun resolveProcessors(refs: List<ProcessorRef>?): MutableList<ProcessorRef>
}

@Service
@Transactional
class PipelineResolverServiceImpl(
    val pipelineDao: PipelineDao
) : PipelineResolverService {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Transactional(readOnly = true)
    override fun resolve(pipeline: String?, modules: List<String>?): ResolvedPipeline {
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
                val addOrRemoves = it.map { mod -> !mod.startsWith("-") }
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
    override fun resolve(): ResolvedPipeline {
        val pipeline = pipelineDao.getDefault()
        return resolve(pipeline)
    }

    @Transactional(readOnly = true)
    override fun resolve(pipeline: Pipeline): ResolvedPipeline {
        return if (pipeline.mode == PipelineMode.MODULAR) {
            val modules = pipelineModService.getByIds(pipeline.modules)
            resolveModular(modules)
        } else {
            resolveCustom(pipeline.processors)
        }
    }

    @Transactional(readOnly = true)
    override fun resolve(id: UUID): ResolvedPipeline {
        return resolve(pipelineService.get(id))
    }

    @Transactional(readOnly = true)
    override fun resolveModular(mods: Collection<String>?, includeStandard: Boolean): ResolvedPipeline {
        return resolveModular(pipelineModService.getByNames(mods ?: listOf()), includeStandard)
    }

    @Transactional(readOnly = true)
    override fun resolveModular(mods: List<PipelineMod>, includeStandard: Boolean): ResolvedPipeline {

        val objectives = mutableSetOf<String>()
        val globalArgs = mutableMapOf<String, Any>("pipeline.objectives" to objectives)

        /**
         * An array of modules already applied.
         */
        val appliedModules = mutableSetOf<String>()

        /**
         * The current pipeline.  This is re-resolved after every pipeline module, which
         * means that ModOps in the same module cannot see modifications made by
         * previous ModOps.
         */
        var currentPipeline = if (includeStandard) {
            getStandardPipeline()
        } else {
            mutableListOf(ProcessorRef("PrependMarker", "none"))
        }

        for (module in mods) {

            /**
             * Append the module objective.
             */
            objectives.add(module.type)

            val builder = RecursivePipelineBuilder(currentPipeline, appliedModules, pipelineModService)
            currentPipeline = builder.applyOps(module)
        }

        val execute = currentPipeline.filterNot { it.className == "PrependMarker" }
        return ResolvedPipeline(execute, globalArgs)
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
    override fun resolveProcessors(refs: List<ProcessorRef>?): MutableList<ProcessorRef> {
        val result = mutableListOf<ProcessorRef>()

        refs?.forEach { ref ->
            result.add(ref)
            ref.execute?.let {
                ref.execute = resolveProcessors(it)
            }
        }
        return result
    }

    @Transactional(readOnly = true)
    override fun resolveCustom(refs: List<ProcessorRef>?): ResolvedPipeline {
        val procs = resolveProcessors(refs)
        return ResolvedPipeline(procs)
    }

    /**
     * TODO: allow replacement with bucket configuration file.
     */
    override fun getStandardPipeline(trimPrependMarker: Boolean): List<ProcessorRef> {
        return listOf(
            ProcessorRef("boonai_core.core.PreCacheSourceFileProcessor", "boonai/plugins-core"),
            ProcessorRef("boonai_core.core.FileImportProcessor", "boonai/plugins-core"),
            ProcessorRef("boonai_core.proxy.ImageProxyProcessor", "boonai/plugins-core"),
            ProcessorRef("boonai_core.proxy.VideoProxyProcessor", "boonai/plugins-core"),
            ProcessorRef("boonai_analysis.boonai.ZviSimilarityProcessor", "boonai/plugins-analysis"),
            ProcessorRef("PrependMarker", "none")
        ).dropLastWhile { trimPrependMarker }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResolverServiceImpl::class.java)
    }
}

/**
 * The RecursivePipelineBuilder class handles applying a single module to a Pipeline.
 * All dependencies in the given module are resolved and applied as well.
 */
class RecursivePipelineBuilder(

    val currentPipeline: List<ProcessorRef>,
    val appliedModules: MutableSet<String>,
    val pipelineModService: PipelineModService
) {

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

    fun parsePipelineFragment(module: String, frag: Any?, force: Boolean): List<ProcessorRef> {
        if (frag == null) {
            return emptyList()
        }
        val result = Json.Mapper.convertValue<List<ProcessorRef>>(frag)
        // Sets the module name
        result.forEach {
            it.module = module
            it.force = force
        }
        return result
    }

    fun applyOps(module: PipelineMod): List<ProcessorRef> {

        if (module.name in appliedModules) {
            return currentPipeline
        }

        /**
         * Builds a parallel array of opts for each ProcessorRef in the pipeline.
         */
        val matchingOps: List<List<ModOp>> = currentPipeline.map { ref ->
            getMatchingOps(ref, module)
        }

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
                                newPipeline.addAll(parsePipelineFragment(module.name, it, module.force))
                            }
                        }
                        ModOpType.ADD_BEFORE -> {
                            op.apply?.let {
                                newPipeline.addAll(parsePipelineFragment(module.name, it, module.force))
                            }
                            newPipeline.add(ref)
                        }
                        ModOpType.LAST -> {
                            newPipeline.add(ref)
                            op.apply?.let {
                                last.addAll(parsePipelineFragment(module.name, it, module.force))
                            }
                        }
                        ModOpType.APPEND -> {
                            newPipeline.add(ref)
                            op.apply?.let {
                                append.addAll(parsePipelineFragment(module.name, it, module.force))
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
                            val frag = parsePipelineFragment(module.name, op.apply, module.force)
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
                                prepend.addAll(parsePipelineFragment(module.name, it, module.force))
                            }
                        }
                        ModOpType.REMOVE -> {
                            // just don't do anything.
                        }
                        ModOpType.REPLACE -> {
                            op.apply?.let {
                                newPipeline.addAll(parsePipelineFragment(module.name, it, module.force))
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
                        ModOpType.DEPEND -> {
                            op.apply?.let {
                                val modNames = Json.Mapper.convertValue<List<String>>(op.apply)
                                val mods = pipelineModService.getByNames(modNames)
                                for (dependMod in mods) {
                                    val builder = RecursivePipelineBuilder(
                                        currentPipeline, appliedModules, pipelineModService
                                    )
                                    builder.applyOps(dependMod)
                                    prepend.addAll(builder.prepend)
                                    append.addAll(builder.append)
                                    last.addAll(builder.last)
                                }
                            }
                        }
                    }
                }
            }
        }

        appliedModules.add(module.name)

        val prependMarker = newPipeline.indexOfFirst { it.className == "PrependMarker" }
        if (prependMarker != -1) {
            newPipeline.addAll(prependMarker, prepend)
        }
        newPipeline.addAll(append)

        // Sort last by name as a way of letting people order.
        last.sortBy { it.module }
        newPipeline.addAll(last)
        return newPipeline
    }
}
