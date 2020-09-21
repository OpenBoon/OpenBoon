package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexToIndexMigrationSpec
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.IndexTaskState
import com.zorroa.archivist.domain.IndexTaskType
import com.zorroa.archivist.domain.ProjectIndexMigrationSpec
import com.zorroa.archivist.domain.ProjectSize
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.security.getProjectId
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
}
