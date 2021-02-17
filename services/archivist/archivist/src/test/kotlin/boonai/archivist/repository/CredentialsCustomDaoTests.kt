package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsType
import boonai.archivist.service.CredentialsService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CredentialsCustomDaoTests : AbstractTest() {

    @Autowired
    lateinit var credentialsCustomDao: CredentialsCustomDao

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Test
    fun getEncryptedBlob() {
        val creds = credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )
        val value = credentialsCustomDao.getEncryptedBlob(creds.id)
        assertTrue(value.matches(Regex("[0-9a-fA-F]+")))
    }

    @Test
    fun updateEncryptedBlob() {
        val creds1 = credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )
        val value1 = credentialsCustomDao.getEncryptedBlob(creds1.id)
        credentialsCustomDao.setEncryptedBlob(creds1.id, "bar")
        val value2 = credentialsCustomDao.getEncryptedBlob(creds1.id)
        assertNotEquals(value1, value2)
    }

    @Test
    fun getEncryptedDataSourceBlob() {
        val creds1 = credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )
        val value1 = credentialsCustomDao.getEncryptedBlob(creds1.id)
        credentialsCustomDao.setEncryptedBlob(creds1.id, "bar")
        val value2 = credentialsCustomDao.getEncryptedBlob(creds1.id)
        assertNotEquals(value1, value2)
    }
}
