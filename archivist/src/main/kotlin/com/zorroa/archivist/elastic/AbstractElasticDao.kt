package com.zorroa.archivist.elastic

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.EsRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractElasticDao {

    protected val logger = LoggerFactory.getLogger(javaClass)

    protected lateinit var esClientCache: EsClientCache
    protected lateinit var elastic: ElasticTemplate

    @Autowired
    fun setApplicationProperties(props: ApplicationProperties) {

    }

    @Autowired
    fun setup(esClientCache: EsClientCache) {
        this.esClientCache = esClientCache
        this.elastic = ElasticTemplate(esClientCache)
    }

    fun refreshIndex() { }

    fun getClient() : EsRestClient {
        return esClientCache[getOrgId()]
    }
}
