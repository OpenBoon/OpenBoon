package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.security.getProjectId
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataSourceDaoTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceJdbcDao: DataSourceJdbcDao

    @Autowired
    lateinit var dataSourceDao: DataSourceDao

    @Test
    fun testSetAndGetCredentials() {
        val id = UUID.randomUUID()
        val spec = DataSource(
            id,
            getProjectId(),
            "test",
            "gs://foo-bar",
            null,
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            "admin",
            "admin"
        )

        val ds = dataSourceDao.saveAndFlush(spec)
        val blob = "ABC123"
        val salt = "ababababababab"
        assertTrue(dataSourceJdbcDao.updateCredentials(ds.id, blob, salt))

        val creds = dataSourceJdbcDao.getCredentials(ds.id)
        assertEquals(blob, creds.blob)
        assertEquals(salt, creds.salt)

    }
}