package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.BoonLibState
import boonai.archivist.domain.BoonLibUpdateSpec
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.JobFilter
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.JobState
import boonai.archivist.domain.JobStateChangeEvent
import boonai.archivist.domain.emptyZpsScripts
import boonai.archivist.repository.BoonLibJdbcDao
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class BoonLibServiceTests : AbstractTest() {

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var boonLibJdbcDao: BoonLibJdbcDao

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var jobService: JobService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testCreate() {
        val lib = boonLibService.createBoonLib(getSpec())
        assertEquals("Test", lib.name)
        assertEquals("A test lib", lib.description)
        assertEquals(BoonLibEntity.Dataset, lib.entity)
    }

    @Test
    fun testUpdate() {
        val lib = boonLibService.createBoonLib(getSpec())
        entityManager.flush()
        boonLibJdbcDao.updateBoonLibState(lib.id, BoonLibState.READY)

        assertEquals("Test", lib.name)
        assertEquals("A test lib", lib.description)
        assertEquals(BoonLibEntity.Dataset, lib.entity)

        val boonLibUpdateSpec =
            BoonLibUpdateSpec(name = "Updated Name", description = "Updated Description")
        boonLibService.updateBoonLib(lib, boonLibUpdateSpec)

        assertEquals("Updated Name", lib.name)
        assertEquals("Updated Description", lib.description)
    }

    @Test
    fun testCreateWithJob() {
        val ds = datasetService.createDataset(DatasetSpec("test", DatasetType.Classification))

        val spec = BoonLibSpec(
            "Test",
            "A test lib",
            BoonLibEntity.Dataset,
            ds.id
        )

        boonLibService.createBoonLib(spec)
        val jobs = jobService.getAll(JobFilter())
        assertEquals(1, jobs.size())
    }

    @Test
    fun testGetBoonLib() {
        val lib1 = boonLibService.createBoonLib(getSpec())
        val lib2 = boonLibService.getBoonLib(lib1.id)
        assertEquals(lib1.id, lib2.id)
    }

    @Test
    fun testFindOne() {
        val lib1 = boonLibService.createBoonLib(getSpec())
        entityManager.flush()
        boonLibJdbcDao.updateBoonLibState(lib1.id, BoonLibState.READY)

        val lib2 = boonLibService.findOneBoonLib(BoonLibFilter(ids = listOf(lib1.id)))
        assertEquals(lib1.id, lib2.id)
    }

    @Test
    fun testSearch() {
        val lib1 = boonLibService.createBoonLib(getSpec())
        entityManager.flush()
        boonLibJdbcDao.updateBoonLibState(lib1.id, BoonLibState.READY)

        val lib2 = boonLibService.findAll(BoonLibFilter(ids = listOf(lib1.id)))
        assertEquals(lib1.id, lib2[0].id)
    }

    @Test
    fun testHandleJobEvent() {
        val lib1 = boonLibService.createBoonLib(getSpec())

        val jspec = JobSpec(
            "Exporting Dataset 'foo' to BoonLib '${lib1.id}'",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job = jobService.create(jspec)

        val event = JobStateChangeEvent(job, JobState.Success, JobState.InProgress)
        boonLibService.handleJobStateChangeEvent(event)
    }

    fun getSpec(): BoonLibSpec {
        val ds = datasetService.createDataset(DatasetSpec("test", DatasetType.Classification))
        val spec = BoonLibSpec(
            "Test",
            "A test lib",
            BoonLibEntity.Dataset,
            ds.id
        )
        return spec
    }
}
