package com.zorroa.archivist.service

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getUserOrNull
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration


/**
 * EventLogConfiguration is a Configuration class which is just a method
 * of populating a MeterRegistryHolder with a MeterRegistry instance at startup.
 */
@Configuration
class EventLogConfiguration @Autowired constructor(meterRegistrty: MeterRegistry)  {
    init {
        MeterRegistryHolder.meterRegistrty = meterRegistrty
    }

}

/**
 * A singleton for storing a MeterRegistry which is accessible from static functions.
 */
object MeterRegistryHolder {

    lateinit var meterRegistrty : MeterRegistry

    fun getTags(vararg tags: Tag) : MutableList<Tag> {
        // TODO: Add organization name.
        val result = mutableListOf<Tag>()

        tags?.let {
            result.addAll(it)
        }
        return result
    }

    /**
     * Increment a counter for the action.
     */
    fun increment(name: String, vararg tags: Tag) {
        meterRegistrty.counter(name.toLowerCase(), getTags(*tags)).increment()
    }

    /**
     * Get a counter for the metric, the counter will be created if it doesn't already exist.
     *
     * @param name: Name of the counter
     * @return A counter counter that can be incremented
     */
    fun counter(name: String): Counter = meterRegistrty.counter(name)
}

/**
 * Format a log message with key value pairs
 *
 * @param obj: The object in question
 * @param action: The object we've took or are about to take
 * @param kvp: A variable arg list of maps which contain additional key/value pairs to log.
 * @return A formatted log string
 */
fun formatLogMessage(obj: LogObject, action: LogAction, vararg kvp: Map<String, Any?>?) : String {
    val user = getUserOrNull()
    val sb = StringBuilder(256)

    sb.append("ZEVENT zorroa.object='${obj.toString().toLowerCase()}' zorroa.action='${action.toString().toLowerCase()}'")
    if (user != null) {
        sb.append(" zorroa.username='${user.getName()}' zorroa.orgId='${user.organizationId}'")
    }
    kvp?.forEach { e->
        e?.forEach {
            if (it.value != null) {
                if (it.value is Number || it.value is Boolean) {
                    sb.append(" zorroa.${it.key}=${it.value}")
                } else {
                    sb.append(" zorroa.${it.key}='${it.value}'")
                }
            }
        }
    }
    return sb.toString()
}

/**
 * Extends the SLF4J Logger class with an event method.
 *
 * @param obj: The object in question
 * @param action: The object we've took or are about to take
 * @param kvp: A map of key value pairs we want to associate with the line.
 */
fun Logger.event(obj: LogObject, action: LogAction, vararg kvp: Map<String, Any?>?) {
    // Don't ever pass kvp into MeterRegistryHolder for tags
    MeterRegistryHolder.increment("zorroa.event.$obj.$action", Tag.of("state", "success"))
    if (this.isInfoEnabled) {
        this.info(formatLogMessage(obj, action, *kvp))
    }
}

/**
 * Extends the SLF4J Logger class with an warnEvent method.
 *
 * @param obj: The object in question
 * @param action: The object we've took or are about to take
 * @param message: The message or exception
 * @param kvp: A map of key value pairs we want to associate with the line.
 */
fun Logger.warnEvent(obj: LogObject, action: LogAction, message: String, kvp: Map<String, Any?>?=null, ex: Exception?=null) {
    // Don't ever pass kvp into MeterRegistryHolder for tags
    MeterRegistryHolder.increment("zorroa.event.$obj.$action", Tag.of("state", "warn"))
    if (this.isWarnEnabled) {
        this.warn(formatLogMessage(obj, action, kvp, mapOf("message" to message)), ex)
    }
}

/**
 * Increments metrics for the given asset Search.
 */
fun applyAssetSearchMetrics(search: AssetSearch) {

    if (search.query != null) {
         MeterRegistryHolder.increment("zorroa.asset.search.query", Tag.of("value", search.query))
    }

    val filter = search.filter
    if(filter.isEmpty) {
        return
    }

     MeterRegistryHolder.increment("zorroa.asset.search.filter")

    if (!filter.exists.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.exists")
    }
    if (!filter.missing.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.missing")
    }
    if (!filter.terms.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.terms")
    }
    if (!filter.prefix.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.prefix")
    }
    if (!filter.range.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.range")
    }
    if (!filter.scripts.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.scripts")
    }
    if (!filter.links.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.links")
    }
    if (!filter.similarity.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.similarity")
    }
    if (!filter.kwconf.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.kwconf")
    }
    if (!filter.geo_bounding_box.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.geo_bounding_box")
    }
    if (!filter.mustNot.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.mustNot")
    }
    if (!filter.must.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.must")
    }
    if (!filter.should.isNullOrEmpty()) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.should")
    }
    if (filter.recursive == true) {
         MeterRegistryHolder.increment("zorroa.asset.search.filter_type.recursive")
    }
}

