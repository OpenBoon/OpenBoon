package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Processor
import com.zorroa.archivist.domain.ProcessorFilter
import com.zorroa.archivist.domain.ProcessorSpec
import com.zorroa.archivist.repository.ProcessorDao
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface ProcessorService {
    fun replaceAll(processors: List<ProcessorSpec>) : Int
    fun getAll(filter: ProcessorFilter) : KPagedList<Processor>
    fun get(id: UUID) : Processor
    fun get(name: String) : Processor
}


@Service
@Transactional
class ProcessorServiceImpl @Autowired constructor(
        val processorDao: ProcessorDao) : ProcessorService {

    override fun replaceAll(processors: List<ProcessorSpec>) : Int {
        processorDao.deleteAll()
        return processorDao.batchCreate(processors)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: ProcessorFilter) : KPagedList<Processor> {
        return processorDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID) : Processor {
        return processorDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun get(name: String) : Processor {
        return processorDao.get(name)
    }
}