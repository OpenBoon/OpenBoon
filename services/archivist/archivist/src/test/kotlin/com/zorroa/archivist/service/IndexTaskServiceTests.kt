package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.repository.IndexRouteDao
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class IndexTaskServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexTaskService: IndexTaskService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Test
    fun testCreateIndexMigrationTask() {
        addTestAssets("images")
        refreshIndex()

        val testSpec = IndexRouteSpec("test", 1)
        val dstRoute = indexRoutingService.createIndexRoute(testSpec)
        val srcRoute = indexRouteDao.getProjectRoute()

        val spec = IndexMigrationSpec(dstRoute.id, srcRoute.id)
        val indexTask = indexTaskService.createIndexMigrationTask(spec)

        assertEquals(dstRoute.id, indexTask.dstIndexRouteId)
        assertEquals(srcRoute.id, indexTask.srcIndexRouteId)
    }
}
