package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Migration
import com.zorroa.archivist.domain.MigrationType
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Created by chambers on 2/3/16.
 */
class MigrationDaoTests : AbstractTest() {

    @Autowired
    internal var migrationDao: MigrationDao? = null

    @Test
    fun testGetAll() {
        val count = jdbc.queryForObject("SELECT COUNT(1) FROM migration", Int::class.java)!!
        assertEquals(count, migrationDao!!.getAll().size)
    }

    @Test
    fun testGetAllByType() {
        val count = jdbc.queryForObject("SELECT COUNT(1) FROM migration WHERE int_type=?", Int::class.java,
                MigrationType.ElasticSearchIndex.ordinal)
        assertEquals(count, migrationDao!!.getAll(MigrationType.ElasticSearchIndex).size)
    }

    @Test
    fun setVersion() {
        for (m in migrationDao!!.getAll()) {
            assertTrue(migrationDao!!.setVersion(m, 1000000))
        }

        for (m in migrationDao!!.getAll()) {
            assertFalse(migrationDao!!.setVersion(m, 1000000))
        }
    }
}
