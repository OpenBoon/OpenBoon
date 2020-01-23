package com.zorroa.archivist.service

import com.zorroa.archivist.search.SearchSourceMapper
import com.zorroa.archivist.security.getProjectId
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface AssetSearchService {
    fun search(search: Map<String, Any>): SearchResponse
}

@Service
class AssetSearchServiceImpl : AssetSearchService {

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    override fun search(search: Map<String, Any>): SearchResponse {
        val client = indexRoutingService.getProjectRestClient()
        val req = client.newSearchRequest()
        req.source(SearchSourceMapper.convert(search))
        req.preference(getProjectId().toString())

        return client.client.search(req, RequestOptions.DEFAULT)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AssetSearchServiceImpl::class.java)
    }
}
