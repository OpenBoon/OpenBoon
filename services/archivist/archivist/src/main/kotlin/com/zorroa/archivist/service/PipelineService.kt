package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsSlot
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.PipelineDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface PipelineService {
    fun resolve(name: String): List<ProcessorRef>
    fun resolve(id: UUID): List<ProcessorRef>
    fun create(spec: PipelineSpec): Pipeline
    fun get(id: UUID): Pipeline
    fun get(name: String): Pipeline
    fun getAll(filter: PipelineFilter): KPagedList<Pipeline>
    fun update(pipeline: Pipeline): Boolean
    fun delete(id: UUID): Boolean
    fun resolve(slot: ZpsSlot, refs: List<ProcessorRef>?): List<ProcessorRef>
    fun findOne(filter: PipelineFilter): Pipeline
}

/**
 * The PipelineService handles the management of pipelines.
 */
@Service
@Transactional
class PipelineServiceImpl @Autowired constructor(
    private val pipelineDao: PipelineDao
) : PipelineService {

    override fun create(spec: PipelineSpec): Pipeline {
        return pipelineDao.create(spec)
    }

    override fun update(pipeline: Pipeline): Boolean {
        return pipelineDao.update(pipeline)
    }

    override fun get(id: UUID): Pipeline {
        return pipelineDao.get(id)
    }

    override fun get(name: String): Pipeline {
        return pipelineDao.get(name)
    }

    override fun getAll(filter: PipelineFilter): KPagedList<Pipeline> {
        return pipelineDao.getAll(filter)
    }

    override fun findOne(filter: PipelineFilter): Pipeline {
        return pipelineDao.findOne(filter)
    }

    override fun delete(id: UUID): Boolean {
        return pipelineDao.delete(id)
    }

    override fun resolve(name: String): List<ProcessorRef> {
        // val processors = mutableListOf<ProcessorRef>()
        val pipeline = pipelineDao.get(name)
        return pipeline.processors
    }

    override fun resolve(id: UUID): List<ProcessorRef> {
        val pipeline = pipelineDao.get(id)
        return resolve(pipeline.slot, pipeline.processors)
    }

    override fun resolve(slot: ZpsSlot, refs: List<ProcessorRef>?): MutableList<ProcessorRef> {
        val result = mutableListOf<ProcessorRef>()

        refs?.forEach { ref ->
            if (ref.className.startsWith("pipeline:", ignoreCase = true)) {
                val name = ref.className.split(":", limit = 2)[1]

                val pl = try {
                    pipelineDao.get(UUID.fromString(name))
                } catch (e: IllegalArgumentException) {
                    pipelineDao.get(name)
                }

                if (pl.slot != slot) {
                    throw throw IllegalArgumentException(
                        "Cannot have pipeline type " +
                            pl.slot + " embedded in a " + slot + " pipeline"
                    )
                }
                result.addAll(resolve(slot, pl.processors))
            } else {
                result.add(ref)
                ref.execute?.let {
                    ref.execute = resolve(slot, it)
                }
            }
        }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
