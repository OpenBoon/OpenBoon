package boonai.common.service.security

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import boonai.common.apikey.ZmlpActor
import boonai.common.service.storage.SystemStorageService
import boonai.common.util.Json
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class EncryptionServiceTests {

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    @Autowired
    lateinit var encryptionService: EncryptionService

    val projectId = UUID.fromString("BED46E21-81F2-4E3D-AF09-18CA5C90A34C")
    lateinit var actor: ZmlpActor

    val mockKeys = listOf(
        "abc", "124", "45a", "76m",
        "1bc", "128", "45b", "76n",
        "2bc", "129", "45c", "76o",
        "3bc", "121", "45d", "76p"
    )

    @Before
    fun setupAuth() {
        actor = ZmlpActor(UUID.randomUUID(), projectId, "test-key", setOf())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(actor, null)
    }

    @Test
    fun testGetProjectKey() {
        whenever(
            systemStorageService.fetchObject(
                any(), eq(Json.LIST_OF_STRING)
            )
        ).thenReturn(mockKeys)

        val key1 = encryptionService.getProjectKey(8)
        assertEquals("3bc1212bc76p", key1)

        val key2 = encryptionService.getProjectKey(1001)
        assertEquals("3bc121129abc", key2)

        val key3 = encryptionService.getProjectKey(4502)
        assertEquals("3bc12145b76p", key3)
    }

    @Test
    fun testGetProjectKeyWithProjectId() {
        whenever(
            systemStorageService.fetchObject(
                any(), eq(Json.LIST_OF_STRING)
            )
        ).thenReturn(mockKeys)

        val key1 = encryptionService.getProjectKey(actor.projectId, 8)
        assertEquals("3bc1212bc76p", key1)

        val key2 = encryptionService.getProjectKey(actor.projectId, 1001)
        assertEquals("3bc121129abc", key2)

        val key3 = encryptionService.getProjectKey(actor.projectId, 4502)
        assertEquals("3bc12145b76p", key3)
    }

    @Test
    fun testEncryptAndDecryptString() {
        whenever(
            systemStorageService.fetchObject(
                any(), eq(Json.LIST_OF_STRING)
            )
        ).thenReturn(mockKeys)
        val crypted = encryptionService.encryptString("bob", 100)
        val decrypted = encryptionService.decryptString(crypted, 100)
        assertEquals("bob", decrypted)
    }

    @Test
    fun testEncryptAndDecryptStringWithProjectId() {
        whenever(
            systemStorageService.fetchObject(
                any(), eq(Json.LIST_OF_STRING)
            )
        ).thenReturn(mockKeys)
        val crypted = encryptionService.encryptString(projectId, "bilbo", 100)
        val decrypted = encryptionService.decryptString(projectId, crypted, 100)
        assertEquals("bilbo", decrypted)
    }
}
