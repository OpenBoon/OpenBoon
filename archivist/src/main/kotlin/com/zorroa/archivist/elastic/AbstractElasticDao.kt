package com.zorroa.archivist.elastic

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.common.clients.EsRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractElasticDao {

    protected val logger = LoggerFactory.getLogger(javaClass)

    protected lateinit var indexRoutingService: IndexRoutingService
    protected lateinit var elastic: ElasticTemplate

    @Autowired
    fun setApplicationProperties(props: ApplicationProperties) {

    }

    @Autowired
    fun setup(routingService: IndexRoutingService) {
        this.indexRoutingService = routingService
        this.elastic = ElasticTemplate(routingService)
    }

    fun refreshIndex() { }

    fun getClient() : EsRestClient {
        return indexRoutingService[getOrgId()]
    }
}
