package boonai.archivist.service

import boonai.archivist.domain.Processor
import boonai.archivist.domain.ProcessorFilter
import boonai.archivist.domain.ProcessorSpec
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.ProcessorDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ProcessorService {
    fun replaceAll(processors: List<ProcessorSpec>): Int
    fun getAll(filter: ProcessorFilter): KPagedList<Processor>
    fun get(id: UUID): Processor
    fun get(name: String): Processor
    fun findOne(filter: ProcessorFilter): Processor
}

@Service
@Transactional
class ProcessorServiceImpl @Autowired constructor(
    val processorDao: ProcessorDao
) : ProcessorService {

    override fun replaceAll(processors: List<ProcessorSpec>): Int {
        processorDao.deleteAll()
        return processorDao.batchCreate(processors)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: ProcessorFilter): Processor {
        return processorDao.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: ProcessorFilter): KPagedList<Processor> {
        return processorDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Processor {
        return processorDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun get(name: String): Processor {
        return processorDao.get(name)
    }
}
