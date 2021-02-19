package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Credentials
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsType
import boonai.archivist.domain.CredentialsUpdate
import boonai.common.service.security.getProjectId
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

    val spec = CredentialsSpec("gcp", CredentialsType.AWS, TEST_AWS_CREDS)

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
        val update = CredentialsUpdate("dog", TEST_AWS_CREDS)
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
        assertEquals(TEST_AWS_CREDS, blob2)
    }

    @Test
    fun testSetEncryptedBlob() {
        entityManager.flush()
        credentialsService.setEncryptedBlob(creds.id, spec.type, TEST_AWS_CREDS)
        val value = credentialsService.getDecryptedBlob(creds.id)
        assertEquals(TEST_AWS_CREDS, value)
    }
}
