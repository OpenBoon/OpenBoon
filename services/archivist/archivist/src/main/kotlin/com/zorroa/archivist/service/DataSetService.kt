package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.repository.DataSetDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

interface DataSetService {

    /**
     * Create a new DataSet.
     */
    fun create(spec: DataSetSpec): DataSet

    /**
     * Get a DataSet by name.
     */
    fun get(name: String): DataSet

    /**
     * Get a DataSet by Id.
     */
    fun get(id: UUID): DataSet
}

@Service
@Transactional
class DataSetServiceImpl(
    val dataSetDao: DataSetDao
) : DataSetService {

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .initialCapacity(10)
        .concurrencyLevel(2)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build(object : CacheLoader<UUID, DataSet>() {
            @Throws(Exception::class)
            override fun load(id: UUID): DataSet {
                return dataSetDao.getOneByProjectIdAndId(getProjectId(), id)
            }
        })

    override fun create(spec: DataSetSpec): DataSet {

        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor()

        val ts = DataSet(
            id,
            getProjectId(),
            spec.name,
            spec.type,
            time,
            time,
            actor.toString(),
            actor.toString()
        )
        dataSetDao.save(ts)
        logger.event(LogObject.DATASET, LogAction.CREATE, mapOf("dataSetId" to id))
        return ts
    }

    @Transactional(readOnly = true)
    override fun get(name: String): DataSet {
        return dataSetDao.getOneByProjectIdAndName(getProjectId(), name)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): DataSet {
        try {
            // can get hit hard during ingests so this is cached
            return cache.get(id)
        } catch (e: ExecutionException) {
           throw e.cause ?: EmptyResultDataAccessException("DataSet Id not found", 1)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSetServiceImpl::class.java)
    }
}