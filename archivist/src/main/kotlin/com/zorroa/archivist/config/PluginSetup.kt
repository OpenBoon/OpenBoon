package com.zorroa.archivist.config

import com.zorroa.archivist.service.PluginService
import com.zorroa.sdk.processor.SharedData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

import java.io.IOException


@Component
class PluginSetup : ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    internal var pluginService: PluginService? = null

    @Autowired
    internal var sharedData: SharedData? = null

    @Value("\${zorroa.cluster.index.alias}")
    private val alias: String? = null

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        /**
         * During unit tests, setupDataSources() is called after the indexes are prepared
         * for a unit test.
         */
        if (!ArchivistConfiguration.unittest) {
            /**
             * Have async threads inherit the current authorization.
             */

            pluginService!!.installAndRegisterAllPlugins()
            pluginService!!.installBundledPipelines()
        }
    }

    @Throws(IOException::class)
    fun setupDataSources() {
        logger.info("Setting up data sources")
        //ElasticClientUtils.createIndexedScripts(client);
        //ElasticClientUtils.createEventLogTemplate(client);
        createSharedPaths()
        refreshIndex()
    }


    fun createSharedPaths() {
        sharedData!!.create()
    }

    fun refreshIndex() {
        logger.info("Refreshing Elastic Indexes")
        //ElasticClientUtils.refreshIndex(client);
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PluginSetup::class.java)
    }
}
