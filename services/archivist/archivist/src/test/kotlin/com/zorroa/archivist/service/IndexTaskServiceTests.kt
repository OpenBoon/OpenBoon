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
import kotlin.test.assertEquals

class IndexTaskServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexTaskService: IndexTaskService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

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
}
