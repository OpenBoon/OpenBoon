package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.IndexTaskState
import com.zorroa.archivist.domain.IndexTaskType
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.service.IndexMigrationService
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
    lateinit var indexMigrationService: IndexMigrationService

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testGet() {
        val testSpec = IndexRouteSpec("test", 1)

        val srcRoute = indexRouteDao.getProjectRoute()
        val route = indexRoutingService.createIndexRoute(testSpec)
        val spec = IndexMigrationSpec(srcRoute.id, route.id)
        val task = indexMigrationService.createIndexMigrationTask(spec)

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
