package boonai.archivist.queue.listener

import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.queue.PubSubAbstractTest
import boonai.archivist.security.getProjectId
import org.junit.After
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.web.context.request.RequestContextHolder

class IndexRoutingListenerTest : PubSubAbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @After
    fun reset() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun testCloseAndDeleteProjectIndex() {
        val spec1 = IndexRouteSpec("test", 1, shards = 1, replicas = 0)
        val ir1 = indexRoutingService.createIndexRoute(spec1)
        indexRoutingService.getAll(IndexRouteFilter(projectIds = listOf(getProjectId())))

        indexRoutingListener.closeAndDeleteProjectIndexes(getProjectId().toString())

        assertThrows<EmptyResultDataAccessException> {
            indexRoutingService.getIndexRoute(ir1.id)
        }
    }
}