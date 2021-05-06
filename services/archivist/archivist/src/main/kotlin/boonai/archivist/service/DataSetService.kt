package boonai.archivist.service

import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.DataSetUpdate
import boonai.archivist.repository.DataSetDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DataSetService {

    fun createDataSet(spec: DataSetSpec): DataSet
    fun getDataSet(id: UUID): DataSet
    fun getDataSet(name: String): DataSet
    fun updateDataSet(dataSet: DataSet, update: DataSetUpdate)
}

@Service
@Transactional
class DataSetServiceImpl(
    val dataSetDao: DataSetDao
) : DataSetService {

    override fun createDataSet(spec: DataSetSpec): DataSet {

        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()

        val ds = DataSet(
            id,
            actor.projectId,
            spec.name,
            spec.type,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        dataSetDao.saveAndFlush(ds)
        return ds
    }

    override fun getDataSet(id: UUID): DataSet {
        return dataSetDao.getOneByProjectIdAndId(getProjectId(), id)
            ?: throw EmptyResultDataAccessException("The DataSet $id does not exist", 1)
    }

    override fun getDataSet(name: String): DataSet {
        return dataSetDao.getOneByProjectIdAndName(getProjectId(), name)
            ?: throw EmptyResultDataAccessException("The DataSet $name does not exist", 1)
    }

    override fun updateDataSet(dataSet: DataSet, update: DataSetUpdate) {
        dataSet.name = update.name
        dataSet.timeModified = System.currentTimeMillis()
        dataSet.actorModified = getZmlpActor().toString()
    }
}
