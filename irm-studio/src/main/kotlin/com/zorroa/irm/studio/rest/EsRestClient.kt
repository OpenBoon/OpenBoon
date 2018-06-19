package com.zorroa.irm.studio.rest

import com.zorroa.irm.studio.domain.IndexRoute
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI
import java.util.*

class EsRestClient(val org: IndexRoute, val client: RestHighLevelClient)

interface IndexRouteClient {
    fun getIndexRoute(orgId: UUID) : IndexRoute
}

@Component
class ESIndexRouteClient : IndexRouteClient {

    val client =  RestClient("http://localhost:8080")

    override fun getIndexRoute(orgId: UUID) : IndexRoute {
        return client.get("/api/v1/index-routes/$orgId", IndexRoute::class.java)
    }
}

@Component
class EsRestClientCache {

    @Autowired
    lateinit var indexRouteClient: IndexRouteClient

    /**
     * Will only be a few of these, no need to worry about eviction currently.
     */
    val cache = mutableMapOf<UUID, EsRestClient>()


    fun getClient(org: UUID) : EsRestClient {
        val route = indexRouteClient.getIndexRoute(org)
        return getClient(route)
    }

    fun getClient(route: IndexRoute) : EsRestClient {
        return cache.getOrPut(route.id, {
            val uri = URI.create(route.clusterUrl)
            return EsRestClient(route, RestHighLevelClient(
                    RestClient.builder(HttpHost(uri.host, uri.port, uri.scheme))))
        })
    }
}
