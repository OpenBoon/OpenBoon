package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRouteSpec

import com.zorroa.archivist.repository.IndexRouteDao
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexTaskMonitorTests : AbstractTest() {

    @Autowired
    lateinit var indexMigrationService: IndexMigrationService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var indexTaskMonitor: IndexTaskMonitor

    @Test
    fun testHandleCompletedReindexTask() {
        addTestAssets("images")
        refreshIndex()

        val testSpec = IndexRouteSpec("english_strict", 1)
        val dstRoute = indexRoutingService.createIndexRoute(testSpec)
        val srcRoute = indexRouteDao.getProjectRoute()

        val spec = IndexMigrationSpec(srcRoute.id, dstRoute.id)
        indexMigrationService.createIndexMigrationTask(spec)

        var completed = false
        for (i in 1..10) {
            if (indexTaskMonitor.handleCompletedTasks() > 0) {
                completed = true
                break
            }
            Thread.sleep(500)
        }
        assertTrue(completed)
        refreshIndex()

        // Do a search to ensure data is in new index
        val rest = indexRoutingService.getProjectRestClient()
        val search = rest.newSearchRequest()
        search.source(SearchSourceBuilder().query(QueryBuilders.matchAllQuery()))
        val hits = rest.client.search(search, RequestOptions.DEFAULT).hits.totalHits.value
        assertEquals(20, hits)
    }
}
