package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.Project
import boonai.archivist.domain.ProjectNameUpdate
import boonai.archivist.domain.ProjectQuotaCounters
import boonai.archivist.domain.ProjectSpec
import boonai.archivist.domain.ProjectTier
import boonai.archivist.domain.ProjectTierUpdate
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.repository.ProjectQuotasDao
import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

class ProjectControllerTests : MockMvcTest() {

    lateinit var testProject: Project
    lateinit var testSpec: ProjectSpec

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var projectQuotasDao: ProjectQuotasDao

    @Before
    fun init() {
        testSpec = ProjectSpec("Zorroa Test")
        testProject = projectService.create(testSpec)
    }

    @Test
    fun testCreate() {
        val testSpec = ProjectSpec("Zorroa Test2")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/projects")
                .headers(admin())
                .content(Json.serialize(testSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/${testProject.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testProject.name)))
            .andExpect(jsonPath("$.id", CoreMatchers.equalTo(testProject.id.toString())))
            .andExpect(jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetAll() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.list.length()", CoreMatchers.equalTo(2)))
            .andExpect(jsonPath("$.page.totalCount", CoreMatchers.equalTo(2)))
            .andExpect(jsonPath("$.page.from", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun findOne() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/_findOne")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("ids" to listOf(testProject.id))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testProject.name)))
            .andExpect(jsonPath("$.id", CoreMatchers.equalTo(testProject.id.toString())))
            .andExpect(jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetMyProject() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("unittest")))
            .andReturn()
    }

    @Test
    fun testGetMyProjecQuotas() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project/_quotas")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.videoSecondsMax", CoreMatchers.anything()))
            .andExpect(jsonPath("$.videoSecondsCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$.deletedVideoSecondsCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$.pageMax", CoreMatchers.anything()))
            .andExpect(jsonPath("$.pageCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$.deletedPageCount", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetMyProjectQuotasTimeSeries() {
        val counters = ProjectQuotaCounters()
        counters.videoClipCount = 1
        counters.pageCount = 1
        counters.videoLength = 10.0
        counters.imageFileCount = 1
        counters.documentFileCount = 1
        counters.videoFileCount = 1

        projectQuotasDao.incrementTimeSeriesCounters(Date(), counters)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project/_quotas_time_series")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].videoSecondsCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$[0].pageCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$[0].deletedVideoSecondsCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$[0].deletedPageCount", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun updateTier() {
        val pid = getProjectId()
        var update = ProjectTierUpdate(ProjectTier.PREMIER)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/projects/$pid/_update_tier")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update))
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.tier",
                    CoreMatchers.anything()
                )
            )
            .andReturn()
    }

    @Test
    fun rename() {
        var update = ProjectNameUpdate("new Name")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/project/_rename")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update))
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.op",
                    CoreMatchers.anything()
                )
            )
            .andReturn()
    }

    @Test
    fun setIndex() {
        val pid = getProjectId()
        val testSpec = IndexRouteSpec(
            "test", 1, shards = 1, replicas = 0
        )
        val route = indexRoutingService.createIndexRoute(testSpec)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/projects/$pid/_index/${route.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.op",
                    CoreMatchers.anything()
                )
            )
            .andExpect(
                jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }
}
