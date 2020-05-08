package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.domain.StandardContainers
import com.zorroa.archivist.domain.SupportedMedia
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
    fun publishModel(model: Model): PipelineMod
}

@Service
@Transactional
class ModelServiceImpl(
    val modelDao: ModelDao,
    val modelJdbcDao: ModelJdbcDao,
    val dataSetDao: DataSetDao,
    val jobLaunchService: JobLaunchService,
    val pipelineModService: PipelineModService
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
            model.type.trainProcessor, "zmlp/plugins-train",
            model.type.trainArgs.plus(
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

    override fun publishModel(model: Model): PipelineMod {
        val mod = pipelineModService.findByName(model.name, false)
        if (mod != null) {
            return mod
        }

        val modspec = PipelineModSpec(
            model.name,
            model.type.description,
            Provider.CUSTOM,
            Category.TRAINED,
            model.type.modType,
            listOf(SupportedMedia.Documents, SupportedMedia.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            model.type.trainProcessor,
                            StandardContainers.TRAIN,
                            model.type.classifyArgs.plus(mapOf("model_file_id" to model.fileId)),
                            module = model.name
                        )
                    )
                )
            )
        )
        return pipelineModService.create(modspec)
    }
}
