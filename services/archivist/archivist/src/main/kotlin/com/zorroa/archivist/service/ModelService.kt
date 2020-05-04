package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.repository.DataSetDao
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.ModelDao
import com.zorroa.archivist.repository.ModelJdbcDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ModelService {

    fun createModel(spec: ModelSpec): Model
    fun trainModel(model: Model, args: ModelTrainingArgs): Job
    fun getModel(id: UUID): Model
    fun find(filter: ModelFilter): KPagedList<Model>
    fun findOne(filter: ModelFilter): Model
}

@Service
@Transactional
class ModelServiceImpl(
    val modelDao: ModelDao,
    val modelJdbcDao: ModelJdbcDao,
    val dataSetDao: DataSetDao,
    val jobLaunchService: JobLaunchService
) : ModelService {

    override fun createModel(spec: ModelSpec): Model {
        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()
        val ds = dataSetDao.getOneByProjectIdAndId(getProjectId(), spec.dataSetId)
        val name = String.format(spec.type.moduleName, ds.name)
        val locator = ProjectFileLocator(
            ProjectStorageEntity.MODELS, id.toString(), spec.type.name.toLowerCase(), "$name.zip"
        )

        val model = Model(
            id,
            getProjectId(),
            spec.dataSetId,
            spec.type,
            name,
            locator.getFileId(),
            "Train $name",
            false,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        return modelDao.saveAndFlush(model)
    }

    @Transactional(readOnly = true)
    override fun getModel(id: UUID): Model {
        return modelDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun find(filter: ModelFilter): KPagedList<Model> {
        return modelJdbcDao.find(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: ModelFilter): Model {
        return modelJdbcDao.findOne(filter)
    }

    override fun trainModel(model: Model, args: ModelTrainingArgs): Job {
        val processor = ProcessorRef(
            model.type.processor, "zmlp/plugins-train",
            model.type.args.plus(
                mutableMapOf(
                    "dataset_id" to model.dataSetId.toString(),
                    "model_type" to model.type.toString(),
                    "file_id" to model.fileId,
                    "name" to model.name,
                    "publish" to args.publish
                )
            )
        )
        return jobLaunchService.launchTrainingJob(
            model.trainingJobName, processor,
            mapOf("index" to false)
        )
    }
}
