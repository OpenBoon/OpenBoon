package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.domain.PipelineSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataSourceServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var piplineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    val spec = DataSourceSpec(
        "dev-data",
        "gs://zorroa-dev-data",
        fileTypes = listOf("jpg")
    )

    @Test
    fun testCreate() {
        val ds = dataSourceService.create(spec)
        assertEquals(spec.name, ds.name)
        assertEquals(spec.fileTypes, ds.fileTypes)
    }

    @Test
    fun testCreateWithModules() {
        piplineModService.updateStandardMods()

        val spec2 = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = listOf("jpg"),
            modules = setOf("zmlp-objects")
        )

        val ds = dataSourceService.create(spec2)
        assertEquals(spec.name, ds.name)
        assertEquals(spec.fileTypes, ds.fileTypes)
        assertTrue(ds.modules.isNotEmpty())
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(1) FROM x_module_datasource", Int::class.java))
    }

    @Test
    fun testCreateWithCredentials() {
        credentialsService.create(CredentialsSpec("test", CredentialsType.AWS, TEST_AWS_CREDS))
        val spec2 = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = listOf("jpg"),
            credentials = setOf("test")
        )

        val ds = dataSourceService.create(spec2)

        entityManager.flush()
        entityManager.clear()

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(1) FROM x_credentials_datasource", Int::class.java))
        assertTrue(ds2.credentials.isNotEmpty())
    }

    @Test
    fun testUpdate() {
        val ds = dataSourceService.create(spec)
        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("jpg"),
            setOf(),
            setOf()
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(update.fileTypes, ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)
    }

    @Test
    fun testUpdateWithModules() {
        piplineModService.updateStandardMods()
        val ds = dataSourceService.create(spec)

        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("jpg"),
            setOf(),
            setOf("zmlp-objects")
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(update.fileTypes, ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)
        assertEquals(1, ds2.modules.size)
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(1) FROM x_module_datasource", Int::class.java))
    }

    @Test
    fun testUpdateWithCredentials() {
        val ds = dataSourceService.create(spec)
        val creds = credentialsService.create(
            CredentialsSpec("test",
                CredentialsType.AWS, TEST_AWS_CREDS)
        )

        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("jpg"),
            setOf(creds.name),
            setOf()
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(update.fileTypes, ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)

        val credsIds = jdbc.queryForList(
            "SELECT pk_credentials FROM x_credentials_datasource WHERE pk_datasource=?",
            UUID::class.java, ds2.id)
        assertTrue(creds.id in credsIds)
    }

    @Test
    fun testDelete() {
        val ds = dataSourceService.create(spec)
        dataSourceService.delete(ds.id)
        entityManager.flush()
        val count = jdbc.queryForObject(
            "SELECT COUNT(1) FROM datasource WHERE pk_datasource=?", Int::class.java, ds.id)
        assertEquals(0, count)
    }

    @Test
    fun testGet() {
        val ds1 = dataSourceService.create(spec)
        val ds2 = dataSourceService.get(ds1.id)
        assertEquals(ds1, ds2)
    }
}
