package com.zorroa.archivist.config

import com.zorroa.archivist.service.PluginService
import com.zorroa.sdk.processor.SharedData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component


@Component
class PluginSetup : ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    internal lateinit var pluginService: PluginService

    @Autowired
    internal lateinit var sharedData: SharedData

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        /**
         * During unit tests, setupDataSources() is called after the indexes are prepared
         * for a unit test.
         */

        sharedData.create()

        if (!ArchivistConfiguration.unittest) {
            /**
             * Have async threads inherit the current authorization.
             */

            pluginService.installAndRegisterAllPlugins()
            pluginService.installBundledPipelines()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PluginSetup::class.java)
    }
}
