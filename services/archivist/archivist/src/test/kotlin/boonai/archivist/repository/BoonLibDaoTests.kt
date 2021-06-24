package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.*
import boonai.archivist.service.BoonLibService
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
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

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testFindOneBoonLib() {
        val lib1 = boonLibService.createBoonLib(spec1)
        val lib2 = boonLibService.createBoonLib(spec2)
        entityManager.flush()

        val result_id = boonLibJdbcDao.findOneBoonLib(BoonLibFilter(ids = listOf(lib1.id)))
        assertEquals(lib1.id, result_id.id)
        assertEquals(lib1.name, result_id.name)

        val result_name = boonLibJdbcDao.findOneBoonLib(BoonLibFilter(names = listOf(lib1.name)))
        assertEquals(lib1.id, result_name.id)
        assertEquals(lib1.name, result_name.name)

        assertThrows<IncorrectResultSizeDataAccessException> {
            boonLibJdbcDao.findOneBoonLib(BoonLibFilter(entities = listOf(BoonLibEntity.Dataset)))
        }

        assertThrows<IncorrectResultSizeDataAccessException> {
            boonLibJdbcDao.findOneBoonLib(BoonLibFilter(states = listOf(BoonLibState.EMPTY)))
        }
    }

    val spec1 = BoonLibSpec(
        "Test",
        BoonLibEntity.Dataset,
        BoonLibEntityType.Classification,
        LicenseType.CC0,
        "A test lib"
    )

    val spec2 = BoonLibSpec(
        "Example",
        BoonLibEntity.Dataset,
        BoonLibEntityType.Classification,
        LicenseType.CC0,
        "An Example"
    )
}
