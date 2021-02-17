package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.repository.AnalystDao
import boonai.archivist.domain.Analyst
import boonai.archivist.domain.AnalystFilter
import boonai.archivist.domain.AnalystSpec
import boonai.archivist.domain.AnalystState
import boonai.archivist.repository.KPagedList
import boonai.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalystControllerTests : MockMvcTest() {

    @Autowired
    lateinit var analystDao: AnalystDao

    lateinit var analyst: Analyst
    lateinit var spec: AnalystSpec

    @Before
    fun init() {
        authenticateAsAnalyst()
        spec = defaultAnalystSpec()
        analyst = analystDao.create(spec)
    }

    @Test
    fun testSearch() {
        // All filter options tested in DAO.
        val filter = AnalystFilter(states = listOf(AnalystState.Up))
        val list = resultForPostContent<KPagedList<Analyst>>(
            "/api/v1/analysts/_search",
            filter
        )
        assertEquals(1, list.size())
    }

    @Test
    fun testFindOneWithEmptyFilter() {
        val result = resultForPostContent<Analyst>(
            "/api/v1/analysts/_findOne",
            AnalystFilter()
        )
        assertEquals(result.id, analyst.id)
    }

    @Test
    fun testFindOneWithIdFilter() {
        val result = resultForPostContent<Analyst>(
            "/api/v1/analysts/_findOne",
            AnalystFilter(ids = listOf(analyst.id))
        )
        assertEquals(analyst.id, result.id)
    }

    @Test
    fun testFindOneFailsOnNotFound() {
        assertClientErrorForPostContent(
            "/api/v1/analysts/_findOne",
            AnalystFilter(ids = listOf(UUID.randomUUID()))
        )
    }

    @Test
    fun testFindOneFailsWhenMultipleFound() {
        val spec2 = defaultAnalystSpec()
        spec2.endpoint = "http://127.0.0.1:5001"
        analystDao.create(spec2)

        assertClientErrorForPostContent(
            "/api/v1/analysts/_findOne",
            AnalystFilter()
        )
    }

    @Test
    fun testGet() {
        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/analysts/${analyst.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val analyst2 = Json.Mapper.readValue<Analyst>(rsp.response.contentAsString, Analyst::class.java)
        assertEquals(analyst.endpoint, analyst2.endpoint)
        assertEquals(analyst.id, analyst2.id)
        assertEquals(analyst.state, analyst2.state)
    }

    @Test
    fun testLockAndUnlock() {
        val rsp = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/analysts/${analyst.id}/_lock?state=locked")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val status = Json.Mapper.readValue<Map<String, Any>>(rsp.response.contentAsString, Json.GENERIC_MAP)
        assertTrue(status["success"] as Boolean)

        val rsp2 = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/analysts/${analyst.id}/_lock?state=unlocked")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val status2 = Json.Mapper.readValue<Map<String, Any>>(rsp2.response.contentAsString, Json.GENERIC_MAP)
        assertTrue(status2["success"] as Boolean)
    }

    @Test
    fun testLockAndUnlockFailure() {
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/analysts/${analyst.id}/_lock?state=sdsdsdsdsds")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/analysts/wedwdsdsds/_lock?state=unlocked")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    private fun defaultAnalystSpec(): AnalystSpec {
        return AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.40.3",
            null
        )
    }
}
