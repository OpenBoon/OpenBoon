package com.zorroa.archivist.service

import com.zorroa.archivist.search.SearchSourceMapper
import com.zorroa.archivist.security.getProjectId
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.ClearScrollResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface AssetSearchService {
    fun search(search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse
    fun search(search: Map<String, Any>): SearchResponse
    fun scroll(scroll: Map<String, String>): SearchResponse
    fun clearScroll(scroll: Map<String, String>): ClearScrollResponse
    fun count(search: Map<String, Any>): Long
}

@Service
class AssetSearchServiceImpl : AssetSearchService {

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    override fun search(search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()

        if (params.containsKey("scroll")) {
            req.scroll(params.getValue("scroll")[0])
        }

        req.source(SearchSourceMapper.convert(search))
        req.preference(getProjectId().toString())

        return rest.client.search(req, RequestOptions.DEFAULT)
    }

    override fun search(search: Map<String, Any>): SearchResponse {
        return search(search, mapOf())
    }

    override fun count(search: Map<String, Any>): Long {
        val copy = search.toMutableMap()
        copy["size"] = 0
        return search(copy).hits.totalHits.value
    }

    override fun scroll(scroll: Map<String, String>): SearchResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = SearchScrollRequest(scroll.getValue("scroll_id"))
        req.scroll(scroll.getValue("scroll"))
        return rest.client.scroll(req, RequestOptions.DEFAULT)
    }

    override fun clearScroll(scroll: Map<String, String>): ClearScrollResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = ClearScrollRequest()
        req.addScrollId(scroll.getValue("scroll_id"))
        return rest.client.clearScroll(req, RequestOptions.DEFAULT)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AssetSearchServiceImpl::class.java)
    }
}
