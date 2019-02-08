package com.zorroa.archivist.service

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getUserOrNull
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
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

    /**
     * Increment a counter for the action.
     */
    fun increment(name: String) {
        val user = getUserOrNull()

        if (user != null) {
            meterRegistrty.counter(name.toLowerCase(), "zorroa.username", user.username,
                    "zorroa.orgId", user.organizationId.toString()).increment()
        }
        else {
            meterRegistrty.counter(name.toLowerCase(),
                    "zorroa.username", "unknown",  "zorroa.orgId", "unknown").increment()
        }
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
    MeterRegistryHolder.increment("zorroa.$obj.$action")
    this.info(formatLogMessage(obj, action, *kvp))
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
    MeterRegistryHolder.increment("zorroa.$obj.$action.warn")
    this.warn(formatLogMessage(obj, action, kvp, mapOf("message" to message)), ex)
}

/**
 * Utility to generate a key-value map of parameters used in an AssetSearch for event logging.
 *
 * @param search an AssetSearch object
 * @return A map of key value pairs representing values for event logging.
 */
fun searchParams(search: AssetSearch) : Map<String, Any?> {
    val map: MutableMap<String, Any?> = mutableMapOf()
    map["search.query"] = search.query
    if(search.filter?.isEmpty == false) {
        map["search.has_filter"] = true
        incrementFilterCounters(search.filter)
    }
    if(search.aggs != null) {
        map["search.has_aggs"] = true
        MeterRegistryHolder.counter("search.aggs").increment()
    }
    return map
}


fun incrementFilterCounters(filter: AssetFilter) {
    if(filter.isEmpty) {
        return
    }

    MeterRegistryHolder.counter("zorroa.search.filter").increment()
    if (!filter.exists.isNullOrEmpty()) {
        incrementCounter("search.exists")
    }
    if (!filter.missing.isNullOrEmpty()) {
        incrementCounter("search.missing")
    }
    if (!filter.terms.isNullOrEmpty()) {
        incrementCounter("search.terms")
    }
    if (!filter.prefix.isNullOrEmpty()) {
        incrementCounter("search.prefix")
    }
    if (!filter.range.isNullOrEmpty()) {
        incrementCounter("search.range")
    }
    if (!filter.scripts.isNullOrEmpty()) {
        incrementCounter("search.scripts")
    }
    if (!filter.links.isNullOrEmpty()) {
        incrementCounter("search.links")
    }
    if (!filter.similarity.isNullOrEmpty()) {
        incrementCounter("search.similarity")
    }
    if (!filter.kwconf.isNullOrEmpty()) {
        incrementCounter("search.kwconf")
    }
    if (!filter.geo_bounding_box.isNullOrEmpty()) {
        incrementCounter("search.geo_bounding_box")
    }
    if (!filter.mustNot.isNullOrEmpty()) {
        incrementCounter("search.mustNot")
    }
    if (!filter.must.isNullOrEmpty()) {
        incrementCounter("search.must")
    }
    if (!filter.should.isNullOrEmpty()) {
        incrementCounter("search.should")
    }
    if (filter.recursive == true) {
        incrementCounter("search.recursive")
    }
}

fun incrementCounter(name: String) {
    MeterRegistryHolder.counter("zorroa.search.filter.$name").increment()
}

