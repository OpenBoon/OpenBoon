package com.zorroa.archivist.service

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.archivist.domain.UserLogSpec
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.ProcessorType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface PipelineService {

    fun getAll(): List<Pipeline>

    fun create(spec: PipelineSpecV): Pipeline

    fun get(id: Int): Pipeline

    fun get(name: String): Pipeline

    fun getStandard(type: PipelineType): Pipeline

    fun exists(name: String): Boolean

    fun getAll(page: Pager): PagedList<Pipeline>

    fun update(id: Int, spec: Pipeline): Boolean

    fun delete(id: Int): Boolean

    fun validateProcessors(pipelineType: PipelineType, refs: List<ProcessorRef>): MutableList<ProcessorRef>

    fun mungePipelines(type: PipelineType, procs: List<ProcessorRef>?): MutableList<ProcessorRef>

    fun isValidPipelineId(value: Any?): Boolean
}

@Service
@Transactional
class PipelineServiceImpl @Autowired constructor(
        val pipelineDao: PipelineDao,
        val event: TransactionEventManager
) : PipelineService {

    @Autowired
    internal lateinit var pluginService: PluginService

    @Autowired
    internal lateinit var logService: EventLogService

    override fun create(spec: PipelineSpecV): Pipeline {
        /**
         * Each processor needs to be validated.
         */
        spec.processors = validateProcessors(spec.type, spec.processors)

        val p = pipelineDao.create(spec)
        event.afterCommit {
            if (SecurityUtils.getAuthentication() != null) {
                logService.logAsync(UserLogSpec.build(LogAction.Create, p))
            }
        }
        return p
    }

    override fun get(id: Int): Pipeline {
        return pipelineDao.get(id)
    }

    override fun get(name: String): Pipeline {
        return pipelineDao.get(name)
    }

    override fun getStandard(type: PipelineType): Pipeline {
        return pipelineDao.getStandard(type)
    }

    override fun exists(name: String): Boolean {
        return pipelineDao.exists(name)
    }

    override fun getAll(): List<Pipeline> {
        return pipelineDao.getAll()
    }

    override fun getAll(page: Pager): PagedList<Pipeline> {
        return pipelineDao.getAll(page)
    }

    override fun update(id: Int, spec: Pipeline): Boolean {
        val pl = pipelineDao.get(id)

        /**
         * TODO: recursively validate all processors.
         */
        val validated = validateProcessors(pl.type, spec.processors)
        spec.processors = validated

        val result = pipelineDao.update(id, spec)
        if (result) {
            event.afterCommit { logService.logAsync(UserLogSpec.build(LogAction.Update, "pipeline", id)) }
        }
        return result
    }

    override fun delete(id: Int): Boolean {
        val result = pipelineDao.delete(id)
        if (result) {
            event.afterCommit { logService.logAsync(UserLogSpec.build(LogAction.Delete, "pipeline", id)) }
        }
        return result
    }

    override fun validateProcessors(pipelineType: PipelineType, refs: List<ProcessorRef>): MutableList<ProcessorRef> {

        val validated = Lists.newArrayList<ProcessorRef>()

        for (ref in refs) {
            val vref = pluginService.getProcessorRef(ref)

            if (!(PipelineType.ALLOWED_PROCESSOR_TYPES as MutableMap<PipelineType, Set<ProcessorType>>).getOrDefault(pipelineType, ImmutableSet.of<ProcessorType>()).contains(vref.type)) {
                throw IllegalStateException("Cannot have processor type " +
                        vref.type + " in a " + pipelineType + " pipeline")
            }
            validated.add(vref)
            if (ref.execute != null) {
                vref.execute = validateProcessors(PipelineType.Import, ref.execute)
            }
        }

        return validated
    }

    override fun mungePipelines(type: PipelineType, procs: List<ProcessorRef>?): MutableList<ProcessorRef> {
        val result = Lists.newArrayListWithCapacity<ProcessorRef>(8)

        if (procs != null) {
            val refs = pluginService.getProcessorRefs(procs)
            refs?.let {
                result.addAll(refs)
            }

        }

        if (result.isEmpty()) {
            try {
                val p = getStandard(type)
                val refs = pluginService.getProcessorRefs(p.id)
                refs?.let { result.addAll(refs)}
            } catch (e: EmptyResultDataAccessException) {
                // ignore the fact there is no standard.
            }
        }
        return result
    }

    /**
     * Return true of the Object is a valid pipeline identifier, which is
     * a number > 0 or a string.
     *
     * @param value
     * @return
     */
    override fun isValidPipelineId(value: Any?): Boolean {
        if (value == null) {
            return false
        }
        if (value is Number) {
            return (value as Int) > 0
        }

        return value is String
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
