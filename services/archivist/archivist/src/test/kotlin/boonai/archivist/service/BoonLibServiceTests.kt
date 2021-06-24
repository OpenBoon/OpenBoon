package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibEntityType
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.JobFilter
import boonai.archivist.domain.LicenseType
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class BoonLibServiceTests : AbstractTest() {

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var jobService: JobService

    val spec = BoonLibSpec(
        "Test",
        BoonLibEntity.Dataset,
        BoonLibEntityType.Classification,
        LicenseType.CC0,
        "A test lib"
    )

    @Test
    fun testCreate() {
        val lib = boonLibService.createBoonLib(spec)
        assertEquals("Test", lib.name)
        assertEquals("A test lib", lib.description)
        assertEquals(BoonLibEntity.Dataset, lib.entity)
    }

    @Test
    fun testCreateWithJob() {
        val ds = datasetService.createDataset(DatasetSpec("test", DatasetType.Classification))

        val spec = BoonLibSpec(
            "Test",
            BoonLibEntity.Dataset,
            BoonLibEntityType.Classification,
            LicenseType.CC0,
            "A test lib",
            ds.id
        )

        boonLibService.createBoonLib(spec)
        val jobs = jobService.getAll(JobFilter())
        assertEquals(1, jobs.size())
    }

    @Test
    fun testGetBoonLib() {
        val lib1 = boonLibService.createBoonLib(spec)
        val lib2 = boonLibService.getBoonLib(lib1.id)
        assertEquals(lib1.id, lib2.id)
    }
}
