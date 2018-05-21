package com.zorroa.archivist.elastic

import com.zorroa.archivist.sdk.config.ApplicationProperties
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractElasticDao {

    protected val logger = LoggerFactory.getLogger(javaClass)

    protected lateinit var client: RestHighLevelClient
    protected lateinit var elastic: ElasticTemplate

    abstract val type: String
    abstract val index: String

    @Autowired
    fun setApplicationProperties(props: ApplicationProperties) {

    }

    @Autowired
    fun setup(client: RestHighLevelClient) {
        this.client = client
        this.elastic = ElasticTemplate(client, index, type)
    }

    fun refreshIndex() {
    }
}
