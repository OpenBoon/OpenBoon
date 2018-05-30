package com.zorroa.archivist.service

import com.zorroa.archivist.domain.AssetId
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.sdk.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Paths

interface AssetService {

    /**
     * Create an asset from the given asset spec.  If the location already exists
     * then reprocess the asset.
     */
    fun analyze(spec: AssetSpec) : AssetId
}

@Service
@Transactional
class AssetServiceInternalImpl @Autowired constructor (
        val assetDao: AssetDao,
        val storageService : StorageService,
        val pipelineService: PipelineService,
        val indexService: IndexService,
        val properties: ApplicationProperties,
        val tx: TransactionEventManager) : AssetService {

    @Autowired
    private lateinit var taskService: TaskService

    override fun analyze(spec: AssetSpec) : AssetId {

        val asset = if (assetDao.exists(spec.location)) {
            assetDao.getId(spec.location!!)
        }
        else {
            create(spec)
        }

        /**
         * Create a task for each pipeline.
         */
        spec.pipelineIds?.forEach {
            val pipeline = pipelineService.get(it)
            val taskSpec = TaskSpec(asset.id, pipeline.id,
                    "pipeline ${pipeline.name} running on ${spec.location}")
            taskService.create(taskSpec)
        }

        return asset
    }

    fun create(spec: AssetSpec) : AssetId {

        if (spec.location != null) {
            if (spec.location.startsWith("/")) {
                if (Files.exists(Paths.get(spec.location))) {
                    spec.directAccess = true
                }
            }

            if (spec.filename == null) {
                spec.filename = FileUtils.filename(spec.location)
            }
        }

        val asset = assetDao.create(spec)
        storageService.createBucket(asset.id.toString())
        return asset
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AssetServiceInternalImpl::class.java)
    }
}

class CoreDataServiceGCPImpl @Autowired constructor () {



}
