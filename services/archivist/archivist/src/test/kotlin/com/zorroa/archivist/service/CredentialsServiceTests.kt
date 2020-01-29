package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Credentials
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.CredentialsUpdate
import com.zorroa.zmlp.service.security.getProjectId
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataRetrievalFailureException
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class CredentialsServiceTests : AbstractTest() {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var credentialsService: CredentialsService

    val blob = """{
            "aws_access_key_id": "your_access_key_id"
            "aws_secret_access_key" : "your_secret_access_key"
        """.trimIndent()

    val spec = CredentialsSpec("gcp", CredentialsType.AWS, blob)

    lateinit var creds: Credentials

    @Before
    fun init() {
        creds = credentialsService.create(spec)
    }

    @Test
    fun testCreate() {
        assertEquals(spec.name, "gcp")
        assertEquals(spec.type, CredentialsType.AWS)
        assertEquals(getProjectId(), creds.projectId)
    }

    @Test(expected = DataIntegrityViolationException::class)
    fun testCreate_duplicateError() {
        credentialsService.create(spec)
    }

    @Test
    fun testGet() {
        val creds2 = credentialsService.get(creds.id)
        assertEquals(creds, creds2)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGet_notFoundError() {
        credentialsService.get(UUID.randomUUID())
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testDelete() {
        credentialsService.delete(creds.id)
        credentialsService.get(creds.id)
    }

    @Test
    fun testUpdate() {
        val update = CredentialsUpdate("dog", "foo:bar")
        val creds2 = credentialsService.update(creds.id, update)
        assertEquals(creds.id, creds2.id)
        assertEquals(update.name, creds2.name)

        val creds = credentialsService.get(creds.id)
        assertEquals("dog", creds.name)
    }

    @Test
    fun testGetDecrypted() {
        entityManager.flush()
        val blob2 = credentialsService.getDecryptedBlob(creds.id)
        assertEquals(blob, blob2)
    }

    @Test
    fun testSetEncryptedBlob() {
        entityManager.flush()
        credentialsService.setEncryptedBlob(creds.id, "booyaa")
        val value = credentialsService.getDecryptedBlob(creds.id)
        assertEquals("booyaa", value)
    }
}
