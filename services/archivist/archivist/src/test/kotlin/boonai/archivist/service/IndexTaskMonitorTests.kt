package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexToIndexMigrationSpec
import boonai.archivist.domain.IndexRouteSpec

import boonai.archivist.repository.IndexRouteDao
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexTaskMonitorTests : AbstractTest() {

    @Autowired
    lateinit var indexTaskService: IndexTaskService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var indexTaskMonitor: IndexTaskMonitor

    @Test
    fun testHandleCompletedReindexTask() {
        addTestAssets("images")
        refreshIndex()

        val testSpec = IndexRouteSpec("english_strict", 5)
        val dstRoute = indexRoutingService.createIndexRoute(testSpec)
        val srcRoute = indexRouteDao.getProjectRoute()

        val spec = IndexToIndexMigrationSpec(srcRoute.id, dstRoute.id)
        indexTaskService.createIndexMigrationTask(spec)

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
        val hits = rest.client.search(search, RequestOptions.DEFAULT).hits.totalHits?.value ?: 0
        assertEquals(20, hits)
    }
}
