package com.zorroa.archivist.service

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.security.getUserOrNull
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

    sb.append("Z-EVENT zorroa.object='${obj.toString().toLowerCase()}' zorroa.action='${action.toString().toLowerCase()}'")
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

    /**
     * Increment a counter for the action.
     */
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
