package boonai.archivist.service

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.BoonLibState
import boonai.archivist.domain.BoonLibUpdateSpec
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.Job
import boonai.archivist.domain.JobState
import boonai.archivist.domain.JobStateChangeEvent
import boonai.archivist.repository.BoonLibDao
import boonai.archivist.repository.BoonLibJdbcDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getZmlpActor
import boonai.archivist.storage.BoonLibStorageService
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.annotation.PostConstruct

interface BoonLibService {

    fun createBoonLib(spec: BoonLibSpec): BoonLib
    fun updateBoonLib(boonlib: BoonLib, spec: BoonLibUpdateSpec)
    fun getBoonLib(id: UUID): BoonLib
    fun importBoonLib(boonlib: BoonLib): BoonLibImportResponse
    fun importBoonLibInto(boonlib: BoonLib, dataset: Dataset): BoonLibImportResponse
    fun findOneBoonLib(boonLibFilter: BoonLibFilter): BoonLib
    fun findAll(boonLibFilter: BoonLibFilter): KPagedList<BoonLib>
    fun handleJobStateChangeEvent(event: JobStateChangeEvent)
}

@Service
class BoonLibServiceImpl(
    val boonLibDao: BoonLibDao,
    val boonLibJdbcDao: BoonLibJdbcDao,
    val datasetService: DatasetService,
    val jobLaunchService: JobLaunchService,
    val boonLibStorageService: BoonLibStorageService,
    val eventBus: EventBus,
) : BoonLibService {

    @PostConstruct
    fun init() {
        // Register for event bus
        eventBus.register(this)
    }

    @Transactional
    override fun createBoonLib(spec: BoonLibSpec): BoonLib {

        val subType = when (spec.entity) {
            BoonLibEntity.Dataset -> {
                val ds = datasetService.getDataset(spec.entityId)
                ds.type.name
            }
        }

        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor().toString()

        val item = BoonLib(
            id,
            spec.name,
            spec.entity,
            subType,
            spec.description,
            BoonLibState.PROCESSING,
            time,
            time,
            actor,
            actor
        )

        boonLibDao.save(item)

        when (spec.entity) {
            BoonLibEntity.Dataset -> {
                val ds = datasetService.getDataset(spec.entityId)
                launchExportJob(ds, item)
            }
        }

        return item
    }

    @Transactional
    override fun updateBoonLib(boonlib: BoonLib, spec: BoonLibUpdateSpec) {

        spec.name?.let {
            boonlib.name = it
        }

        spec.description?.let {
            boonlib.description = it
        }

        boonlib.timeModified = System.currentTimeMillis()
        boonlib.actorModified = getZmlpActor().toString()
    }

    @Transactional(readOnly = true)
    override fun getBoonLib(id: UUID): BoonLib {
        return boonLibDao.getOne(id)
    }

    fun launchExportJob(dataset: Dataset, boonlib: BoonLib): Job {
        return jobLaunchService.launchBoonLibDatasetExport(dataset, boonlib)
    }

    override fun importBoonLib(boonlib: BoonLib): BoonLibImportResponse {
        when (boonlib.entity) {
            BoonLibEntity.Dataset -> {
                val dspec = DatasetSpec(
                    "BoonLib ${boonlib.name}",
                    DatasetType.valueOf(boonlib.entityType),
                    boonlib.description
                )
                val ds = datasetService.createDataset(dspec)
                return importBoonLibInto(boonlib, ds)
            }
        }
    }

    override fun importBoonLibInto(boonlib: BoonLib, dataset: Dataset): BoonLibImportResponse {
        return boonLibStorageService.importAssetsInto(boonlib, dataset)
    }

    @Transactional(readOnly = true)
    override fun findOneBoonLib(boonLibFilter: BoonLibFilter): BoonLib {
        return boonLibJdbcDao.findOneBoonLib(boonLibFilter)
    }

    @Transactional(readOnly = true)
    override fun findAll(boonLibFilter: BoonLibFilter): KPagedList<BoonLib> {
        return boonLibJdbcDao.findAll(boonLibFilter)
    }

    @Subscribe
    override fun handleJobStateChangeEvent(event: JobStateChangeEvent) {
        if (event.newState == JobState.Success) {
            try {
                JOB_REGEX.matchEntire(event.job.name)?.let {
                    val id = UUID.fromString(it.groupValues[1])
                    boonLibJdbcDao.updateBoonLibState(id, BoonLibState.READY)
                    logger.info("Updating boonlib $id to READY")
                }
            } catch (e: Exception) {
                logger.error("Unable to parse BoonLib job name: ${event.job.name}", e)
            }
        }
    }

    companion object {
        val JOB_REGEX = Regex("Exporting .*? '.*?' to BoonLib '(.*?)'")

        private val logger = LoggerFactory.getLogger(BoonLibServiceImpl::class.java)
    }
}
