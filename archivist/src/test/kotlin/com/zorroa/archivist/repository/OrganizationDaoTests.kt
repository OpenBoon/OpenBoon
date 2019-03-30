package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.OrganizationFilter
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.OrganizationUpdateSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganizationDaoTests : AbstractTest() {

    val organizationSpec = OrganizationSpec("BobsBurgers")

    @Autowired
    lateinit var organizationDao: OrganizationDao

    @Test
    fun testCreate() {
        val org = organizationDao.create(organizationSpec)
        assertEquals(organizationSpec.name, org.name)
    }


    @Test
    fun testGet() {
        val org1 = organizationDao.create(organizationSpec)
        val org2 = organizationDao.get(org1.id)
        assertEquals(org1.id, org2.id)
        assertEquals(org1.name, org2.name)
    }

    @Test
    fun testUpdate() {
        val org1 = organizationDao.create(organizationSpec)
        val updateSpec = OrganizationUpdateSpec("Bilbo")
        assertTrue(organizationDao.update(org1, updateSpec))
        val org2 =  organizationDao.get(org1.id)
        assertEquals(updateSpec.name, org2.name)
    }

    @Test
    fun testFindOne() {
        val org1 = organizationDao.create(organizationSpec)
        val org2 =  organizationDao.findOne(OrganizationFilter(ids=listOf(org1.id)))
        assertEquals(org1.id, org2.id)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testFindOneFailure() {
        organizationDao.findOne(OrganizationFilter(ids=listOf(UUID.randomUUID())))
    }

    @Test
    fun testGetAllFiltered() {
        val filter = OrganizationFilter(
                ids=listOf(UUID.randomUUID(), UUID.randomUUID()),
                names=listOf("foo", "bar", "jones")
        )
        // Tests all the filter columns can be in the query
        assertEquals(0, organizationDao.getAll(filter).size())
    }

    @Test
    fun testGetAllSorted() {
        // Just test the DB allows us to sort on each defined sortMap col
        for (field in OrganizationFilter().sortMap.keys) {
            var filter = OrganizationFilter().apply {
                sort = listOf("$field:a")
            }
            val page = organizationDao.getAll(filter)
            assertTrue(page.size() > 0)
        }
    }
}