package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.IndexToIndexMigrationSpec
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexTaskState
import boonai.archivist.domain.IndexTaskType
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.service.IndexTaskService
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class IndexTaskControllerTests : MockMvcTest() {

    @Value("\${archivist.es.url}")
    lateinit var esUrl: String

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var indexTaskService: IndexTaskService

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun getEsTaskInfo() {
        val testSpec = IndexRouteSpec("test", 1)
        val route = indexRoutingService.createIndexRoute(testSpec)
        val spec = IndexToIndexMigrationSpec(indexRouteDao.getProjectRoute().id, route.id)
        val task = indexTaskService.createIndexMigrationTask(spec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-tasks/${task.id}/_es_task_info")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.action",
                    CoreMatchers.equalTo("indices:data/write/reindex")
                )
            )
            .andReturn()
    }

    @Test
    fun testGetAllRunning() {
        val testSpec = IndexRouteSpec("test", 1)
        val route = indexRoutingService.createIndexRoute(testSpec)
        val spec = IndexToIndexMigrationSpec(indexRouteDao.getProjectRoute().id, route.id)
        indexTaskService.createIndexMigrationTask(spec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-tasks")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.length()",
                    CoreMatchers.equalTo(1)
                )
            )
            .andReturn()
    }

    @Test
    fun testGet() {
        val testSpec = IndexRouteSpec("test", 1)
        val srcRoute = indexRouteDao.getProjectRoute()
        val route = indexRoutingService.createIndexRoute(testSpec)
        val spec = IndexToIndexMigrationSpec(srcRoute.id, route.id)
        val task = indexTaskService.createIndexMigrationTask(spec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-tasks/${task.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.srcIndexRouteId",
                    CoreMatchers.equalTo(srcRoute.id.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.dstIndexRouteId", CoreMatchers.equalTo(route.id.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.type", CoreMatchers.equalTo(IndexTaskType.REINDEX.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.state",
                    CoreMatchers.equalTo(IndexTaskState.RUNNING.toString())
                )
            )
            .andReturn()
    }
}
