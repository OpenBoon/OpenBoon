package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.Credentials
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsType
import boonai.archivist.service.CredentialsService
import io.micrometer.core.instrument.util.StringEscapeUtils
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class CredentialsControllerTests : MockMvcTest() {

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
        val payload =
            """
            {
                "name": "gcp_service_account",
                "type": "aws",
                "blob": "${StringEscapeUtils.escapeJson(TEST_AWS_CREDS)}"
            }
            """

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/credentials")
                .headers(admin())
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("gcp_service_account")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.blob").doesNotExist())
            .andReturn()
    }

    @Test
    fun testGet() {

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/credentials/${creds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("gcp")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.blob").doesNotExist())
            .andReturn()
    }

    @Test
    fun testDelete() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/credentials/${creds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(creds.id.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo("AWS")))
            .andReturn()
    }

    @Test
    fun testUpdate_noBlob() {
        val payload =
            """
            {
                "name": "booya"
            }
            """.trimIndent()
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/credentials/${creds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(payload)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("booya")))
            .andReturn()
    }

    @Test
    fun testUpdate_withBlob() {
        val payload =
            """
            {
                "name": "booya",
                "blob": "${StringEscapeUtils.escapeJson(TEST_AWS_CREDS)}"
            }
            """
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/credentials/${creds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(payload)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("booya")))
            .andReturn()

        authenticate(creds.projectId)
        assertEquals(TEST_AWS_CREDS, credentialsService.getDecryptedBlob(creds.id))
    }

    @Test
    fun testDownloadAccessDenied() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/credentials/${creds.id}/_download")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    fun testDownload() {
        val res = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/credentials/${creds.id}/_download")
                .headers(job())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val blob2 = res.response.contentAsString
        assertEquals(TEST_AWS_CREDS, blob2)
    }
}
