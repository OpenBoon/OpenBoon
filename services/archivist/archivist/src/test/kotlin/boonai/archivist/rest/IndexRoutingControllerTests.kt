package boonai.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.MockMvcTest
import boonai.archivist.domain.IndexToIndexMigrationSpec
import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexRouteSimpleSpec
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexRouteState
import boonai.archivist.domain.IndexTaskState
import boonai.archivist.domain.IndexTaskType
import boonai.archivist.domain.ProjectSize
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import kotlin.test.assertEquals

class IndexRoutingControllerTests : MockMvcTest() {

    @Value("\${archivist.es.url}")
    lateinit var esUrl: String

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    val testSpec = IndexRouteSpec(
        "test", 1, shards = 1, replicas = 0
    )

    @Test
    fun testCreate() {
        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(testSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals(esUrl, result.clusterUrl)
        assertEquals("test", result.mapping)
        assertEquals(1, result.majorVer)
        assertEquals(0, result.replicas)
        assertEquals(1, result.shards)
    }

    @Test
    fun testCreateV2() {
        val spec = IndexRouteSimpleSpec(ProjectSize.LARGE)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v2/index-routes")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.replicas", CoreMatchers.equalTo(1)))
            .andExpect(jsonPath("$.shards", CoreMatchers.equalTo(5)))
            .andReturn()
    }

    @Test
    fun testGet() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals(esUrl, result.clusterUrl)
        assertEquals("test", result.mapping)
        assertEquals(1, result.majorVer)
        assertEquals(0, result.replicas)
        assertEquals(1, result.shards)
    }

    @Test
    fun testGetAttrs() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}/_attrs")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.health", CoreMatchers.equalTo("green")))
            .andExpect(jsonPath("$.status", CoreMatchers.equalTo("open")))
            .andReturn()
    }

    @Test
    fun testMigrate() {
        val srcRoute = indexRouteDao.getProjectRoute()
        val route = indexRoutingService.createIndexRoute(testSpec)
        val spec = IndexToIndexMigrationSpec(srcRoute.id, route.id)
        val pid = getProjectId()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes/_migrate")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.projectId", CoreMatchers.equalTo(pid.toString())))
            .andExpect(jsonPath("$.srcIndexRouteId", CoreMatchers.equalTo(srcRoute.id.toString())))
            .andExpect(jsonPath("$.dstIndexRouteId", CoreMatchers.equalTo(route.id.toString())))
            .andExpect(jsonPath("$.type", CoreMatchers.equalTo(IndexTaskType.REINDEX.toString())))
            .andExpect(jsonPath("$.state", CoreMatchers.equalTo(IndexTaskState.RUNNING.toString())))
            .andReturn()
    }

    @Test
    fun testClose() {
        val route = indexRoutingService.createIndexRoute(testSpec)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/index-routes/${route.id}/_close")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()

        val state = indexRoutingService.getIndexRoute(route.id).state
        assertEquals(IndexRouteState.CLOSED, state)
    }

    @Test
    fun testOpen() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        indexRoutingService.closeIndex(route)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/index-routes/${route.id}/_open")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()

        val state = indexRoutingService.getIndexRoute(route.id).state
        assertEquals(IndexRouteState.READY, state)
    }
}
