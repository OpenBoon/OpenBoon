package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexTaskState
import boonai.archivist.domain.IndexTaskType
import boonai.archivist.domain.IndexToIndexMigrationSpec
import boonai.archivist.domain.ProjectIndexMigrationSpec
import boonai.archivist.domain.ProjectSize
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IndexTaskServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexTaskService: IndexTaskService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Test
    fun testCreateIndexToIndexMigrationTask() {
        addTestAssets("images")
        refreshIndex()

        val testSpec = IndexRouteSpec("test", 1)
        val dstRoute = indexRoutingService.createIndexRoute(testSpec)
        val srcRoute = indexRouteDao.getProjectRoute()

        val spec = IndexToIndexMigrationSpec(srcRoute.id, dstRoute.id)
        val indexTask = indexTaskService.createIndexMigrationTask(spec)

        assertEquals(dstRoute.id, indexTask.dstIndexRouteId)
        assertEquals(srcRoute.id, indexTask.srcIndexRouteId)
    }

    @Test
    fun testMigrateProject() {
        addTestAssets("images")
        refreshIndex()

        val project = projectService.get(getProjectId())
        val spec = ProjectIndexMigrationSpec("english_strict", 2, size = ProjectSize.LARGE)
        val task = indexTaskService.migrateProject(project, spec)

        assertEquals(getProjectId(), task.projectId)
        assertEquals(indexRouteDao.getProjectRoute().id, task.srcIndexRouteId)
        assertEquals(task.type, IndexTaskType.REINDEX)
        assertEquals(task.state, IndexTaskState.RUNNING)
    }

    @Test
    fun testMigrateProjectV4ToV5() {
        val project = projectService.get(getProjectId())

        // Make new v4 index.
        val rspec = IndexRouteSpec("english_strict", 4, shards = 1, replicas = 0)
        val route = indexRoutingService.createIndexRoute(rspec)
        projectService.setIndexRoute(project, route)

        addTestAssets("images")
        addTestAssets("video")
        refreshIndex()

        val spec = ProjectIndexMigrationSpec("english_strict", 5, size = ProjectSize.XSMALL)
        val task = indexTaskService.migrateProject(project, spec)
        // Sleep while task completes

        Thread.sleep(5000)

        val newRoute = indexRoutingService.getIndexRoute(task.dstIndexRouteId as UUID)
        projectService.setIndexRoute(project, newRoute)
        indexRoutingService.setIndexRefreshInterval(newRoute, "5s")
        refreshIndex()

        val image = getSample(1, type = "image")[0]
        assertEquals(100, image.getAttr("media.pageNumber"))
        assertEquals("ABC123", image.getAttr("media.pageStack"))
        assertNull(image.getAttr("clip"))

        val video = getSample(1, type = "video")[0]
        assertEquals("video", video.getAttr("deepSearch.name"))
    }

    @Test
    fun testMigrateProjectV5ToV6() {
        val project = projectService.get(getProjectId())

        val analysis = mapOf(
            "zvi-image-similarity" to
                mapOf("simhash" to "12345ABCED", "type" to "similarity")
        )

        // Make new v4 index.
        val rspec = IndexRouteSpec("english_strict", 5, shards = 1, replicas = 0)
        val route = indexRoutingService.createIndexRoute(rspec)
        projectService.setIndexRoute(project, route)

        addTestAssets("images", analysis)
        addTestAssets("video", analysis)
        refreshIndex()

        val spec = ProjectIndexMigrationSpec("english_strict", 6, size = ProjectSize.XSMALL)
        val task = indexTaskService.migrateProject(project, spec)
        // Sleep while task completes

        Thread.sleep(6000)

        val newRoute = indexRoutingService.getIndexRoute(task.dstIndexRouteId as UUID)
        projectService.setIndexRoute(project, newRoute)
        indexRoutingService.setIndexRefreshInterval(newRoute, "5s")
        refreshIndex()
        Thread.sleep(1000)

        val image = getSample(1, type = "image")[0]
        assertEquals("12345ABCED", image.getAttr("analysis.boonai-image-similarity.simhash"))
        assertEquals("similarity", image.getAttr("analysis.boonai-image-similarity.type"))
    }

    @Test
    fun testMigrateProjectV6ToV7() {
        val project = projectService.get(getProjectId())

        val analysis = mapOf(
            "zvi-foo" to
                mapOf(
                    "label" to "cats",
                    "score" to 0.991,
                    "type" to "single-label"
                )
        )

        // Make new v4 index.
        val rspec = IndexRouteSpec("english_strict", 6, shards = 1, replicas = 0)
        val route = indexRoutingService.createIndexRoute(rspec)
        projectService.setIndexRoute(project, route)

        addTestAssets("images", analysis)
        refreshIndex()

        val spec = ProjectIndexMigrationSpec("english_strict", 7, size = ProjectSize.XSMALL)
        val task = indexTaskService.migrateProject(project, spec)
        // Sleep while task completes

        Thread.sleep(5000)

        val newRoute = indexRoutingService.getIndexRoute(task.dstIndexRouteId as UUID)
        projectService.setIndexRoute(project, newRoute)
        indexRoutingService.setIndexRefreshInterval(newRoute, "5s")
        refreshIndex()
        Thread.sleep(1000)

        val image = getSample(1, type = "image")[0]
        assertEquals("labels", image.getAttr("analysis.zvi-foo.type"))
        val preds = image.getAttr(
            "analysis.zvi-foo.predictions",
            Json.LIST_OF_GENERIC_MAP
        ) ?: throw Exception("no preds")

        val pred = preds[0]
        assertEquals(pred["score"], 0.991)
        assertEquals(pred["label"], "cats")
        assertEquals(pred["occurrences"], 1)
    }

    @Test
    fun testMigrateProjectV7ToV8() {
        val project = projectService.get(getProjectId())

        val labels = listOf(
            mapOf(
                "label" to "cats",
                "modelId" to "abc123"
            )
        )

        // Make new v4 index.
        val rspec = IndexRouteSpec("english_strict", 7, shards = 1, replicas = 0)
        val route = indexRoutingService.createIndexRoute(rspec)
        projectService.setIndexRoute(project, route)

        addTestAssets("images", labels = labels)
        refreshIndex()

        val spec = ProjectIndexMigrationSpec("english_strict", 8, size = ProjectSize.XSMALL)
        val task = indexTaskService.migrateProject(project, spec)
        // Sleep while task completes

        Thread.sleep(5000)

        val newRoute = indexRoutingService.getIndexRoute(task.dstIndexRouteId as UUID)
        projectService.setIndexRoute(project, newRoute)
        indexRoutingService.setIndexRefreshInterval(newRoute, "5s")
        refreshIndex()
        Thread.sleep(1000)

        val image = getSample(1, type = "image")[0]
        val imageLabels = image.getAttr<List<Map<String, Any>>>("labels") ?: throw RuntimeException("no labels")
        assertEquals("abc123", imageLabels[0]["dataSetId"])
        assertEquals("cats", imageLabels[0]["label"])
    }
}
