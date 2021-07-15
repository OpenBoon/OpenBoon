package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.service.BoonLibService
import boonai.archivist.service.DatasetService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class BoonLibDaoTests : AbstractTest() {
    @Autowired
    lateinit var boonLibDao: BoonLibDao

    @Autowired
    lateinit var boonLibJdbcDao: BoonLibJdbcDao

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var datasetService: DatasetService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testFindOneBoonLib() {
        val lib1 = makeBoonLib("Test")

        val result_id = boonLibJdbcDao.findOneBoonLib(BoonLibFilter(ids = listOf(lib1.id)))
        assertEquals(lib1.id, result_id.id)
        assertEquals(lib1.name, result_id.name)

        val result_name = boonLibJdbcDao.findOneBoonLib(BoonLibFilter(names = listOf(lib1.name)))
        assertEquals(lib1.id, result_name.id)
        assertEquals(lib1.name, result_name.name)

        boonLibJdbcDao.findOneBoonLib(BoonLibFilter(entities = listOf(BoonLibEntity.Dataset)))
    }

    @Test
    fun testFindAll() {
        val lib1 = makeBoonLib("Test")
        val lib2 = makeBoonLib("Example")

        val result = boonLibJdbcDao.findAll(BoonLibFilter(entities = listOf(BoonLibEntity.Dataset)))
        assertEquals(lib2.name, result[0].name)
        assertEquals(lib1.name, result[1].name)

        val result1 = boonLibJdbcDao.findAll(BoonLibFilter(names = listOf("Test")))
        assertEquals(lib1.name, result1[0].name)
        assertEquals(1, result1.size())
    }

    fun makeBoonLib(name: String): BoonLib {
        val ds = datasetService.createDataset(DatasetSpec(name, DatasetType.Classification, "foo"))
        val spec = BoonLibSpec(
            name,
            "foo",
            BoonLibEntity.Dataset,
            ds.id
        )
        val item = boonLibService.createBoonLib(spec)
        entityManager.flush()
        return item
    }
}
