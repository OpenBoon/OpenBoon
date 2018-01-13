package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.base.Splitter
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Setting
import com.zorroa.archivist.domain.SettingsFilter
import com.zorroa.archivist.repository.SettingsDao
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.sdk.client.exception.ArchivistReadException
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.client.exception.MissingElementException
import com.zorroa.sdk.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

interface SettingsService {

    fun getAll(): List<Setting>

    fun setAll(values: Map<String, Any>): Int

    fun set(key: String, value: Any): Boolean

    fun getAll(filter: SettingsFilter): List<Setting>

    fun get(name: String): Setting

    companion object {
        val ListOfSettingsType: TypeReference<List<Setting>> = object : TypeReference<List<Setting>>() {}
    }

}


@Service
class SettingsServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val settingsDao: SettingsDao
): SettingsService, ApplicationListener<ContextRefreshedEvent> {

    // a memoizer would be nicer but no good ones that allow manual invalidation
    private val settingsCache = CacheBuilder.newBuilder()
            .maximumSize(2)
            .initialCapacity(2)
            .concurrencyLevel(1)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(object : CacheLoader<Int, List<Setting>>() {
                @Throws(Exception::class)
                override fun load(key: Int): List<Setting> {
                    return settingsProvider()
                }
            })


    class SettingValidator(var type: TypeReference<*>, var json: Boolean)


    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        /**
         * Restore the runtime settings.
         */
        val settings = settingsDao.getAll()
        for ((key, value) in settings) {
            System.setProperty(key, value)
        }
    }

    override fun setAll(values: Map<String, Any>): Int {
        var result = 0
        for ((key, value) in values) {

            if (set(key, value, false)) {
                result++
            }
        }
        settingsCache.invalidateAll()
        return result
    }

    override fun getAll(filter: SettingsFilter): List<Setting> {
        if (!SecurityUtils.hasPermission("group::administrator",
                "group::developer")) {
            filter.isLiveOnly = true
        }
        try {
            return settingsCache.get(0).stream()
                    .filter { s -> filter.matches(s) }
                    .limit(filter.count.toLong())
                    .collect(Collectors.toList())
        } catch (e: ExecutionException) {
            throw ArchivistReadException(e)
        }

    }

    override fun getAll(): List<Setting> {
        return getAll(SettingsFilter())
    }

    override fun get(key: String): Setting {
        val filter = SettingsFilter()
        filter.count = 1
        filter.names = ImmutableSet.of(key)
        if (!SecurityUtils.hasPermission("group::administrator", "group::developer")) {
            filter.isLiveOnly = true
        }
        try {
            return getAll(filter)[0]
        } catch (e: IndexOutOfBoundsException) {
            throw MissingElementException("Setting not found: " + key, e)
        }

    }


    fun checkValid(key: String, value: Any?): SettingValidator {
        for ((key1, value1) in WHITELIST) {

            if (key1 == key) {
                try {
                    Json.Mapper.convertValue<Any>(value, value1.type)
                    return value1
                } catch (e: Exception) {
                    throw ArchivistWriteException(
                            "Invalid value for $key, invalid type.")
                }

            }
        }
        throw ArchivistWriteException(
                "Failed to set value for key $key, invalid key.")
    }

    override fun set(key: String, value: Any): Boolean {
        return set(key, value, true)
    }

    operator fun set(key: String, value: Any?, invalidate: Boolean): Boolean {
        val valid = checkValid(key, value)

        logger.info("{} changed to {} by {}", key, value, SecurityUtils.getUsername())

        val result: Boolean
        if (value == null) {
            result = settingsDao.unset(key)
            System.clearProperty(key)
        } else {
            val strVal: String
            if (valid.json) {
                strVal = Json.serializeToString(value)
            } else {
                strVal = toString()
            }
            result = settingsDao.set(key, strVal)
            System.setProperty(key, strVal)
        }
        if (invalidate) {
            settingsCache.invalidateAll()
        }
        return result
    }

    private fun isLive(key: String): Boolean {
        return WHITELIST.keys.contains(key)
    }

    fun settingsProvider(): List<Setting> {

        val resource = ClassPathResource("/application.properties")
        val result = Lists.newArrayListWithExpectedSize<Setting>(64)

        var description: String? = null
        var category: String? = null
        var property: String
        var value: String?

        try {
            InputStreamReader(resource.inputStream, Charset.forName("UTF-8")).use { isr ->
                BufferedReader(isr).use { br ->
                    br.forEachLine { line->
                        if (line.startsWith("###")) {
                            description = if (description != null) {
                                description + line.substring(3)
                            } else {
                                line.substring(3)
                            }
                            description = description!!.trim { it <= ' ' }
                        } else if (line.startsWith("##")) {
                            category = line.substring(2).trim { it <= ' ' }

                        } else if (!line.startsWith("#") && line.contains("=")) {
                            val e = Splitter.on('=').trimResults().omitEmptyStrings().splitToList(line)
                            property = e[0]
                            value = if (property.contains("password") || property.contains("secret")) {
                                "<HIDDEN>"
                            } else {
                                try {
                                    e[1]
                                } catch (ex: IndexOutOfBoundsException) {
                                    null
                                }

                            }

                            val currentValue: String
                            currentValue = if ("<HIDDEN>" == value) {
                                "<HIDDEN>"
                            } else {
                                properties.getString(property)
                            }

                            val s = Setting()
                            s.name = property
                            s.defaultValue = value
                            s.currentValue = currentValue
                            s.isLive = isLive(property)
                            s.category = category
                            s.description = description
                            result.add(s)

                            description = null
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.warn("Failed to read default properties file,", e)
        }

        return result
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SettingsServiceImpl::class.java)

        /**
         * A whitelist of property names that can be set via the API.
         */
        private val WHITELIST = ImmutableMap.builder<String, SettingValidator>()
                .put("archivist.search.keywords.static.fields",
                        SettingValidator(object : TypeReference<Map<String, Double>>() {

                        }, true))
                .put("archivist.search.keywords.auto.fields",
                        SettingValidator(object : TypeReference<String>() {

                        }, false))
                .put("archivist.search.keywords.auto.enabled",
                        SettingValidator(object : TypeReference<Boolean>() {

                        }, false))
                .put("archivist.export.dragTemplate",
                        SettingValidator(object : TypeReference<String>() {

                        }, false))
                .put("archivist.export.videoStreamExtensionFallbackOrder",
                        SettingValidator(object : TypeReference<String>() {

                        }, false))
                .put("archivist.search.sortFields",
                        SettingValidator(object : TypeReference<String>() {

                        }, false))
                .build()
    }


}
