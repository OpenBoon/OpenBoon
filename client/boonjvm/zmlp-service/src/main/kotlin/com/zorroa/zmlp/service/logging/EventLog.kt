package com.zorroa.zmlp.service.logging

import com.zorroa.zmlp.service.security.getZmlpActorOrNull
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

/**
 * All the different actions we can take against any data within the Archivist.
 */
enum class LogAction {
    CREATE,
    REPLACE,
    UPDATE,
    DELETE,
    WRITE,
    GET,
    INDEX,
    EXECUTE,
    AUTHENTICATE,
    AUTHORIZE,
    SECURE,
    BATCH_CREATE,
    BATCH_UPDATE,
    BATCH_DELETE,
    BATCH_INDEX,
    ERROR,
    WARN,
    STATE_CHANGE,
    UPLOAD,
    KILL,
    STREAM,
    EXPAND,
    SCAN,
    DECRYPT,
    SIGN_FOR_WRITE,
    SIGN_FOR_READ,
    ENABLE,
    DISABLE,
    RESOLVE
}

/**
 * All the different classes we're creating event logs for.
 */
enum class LogObject {
    ASSET,
    INDEX_ROUTE,
    INDEX_CLUSTER,
    JOB,
    PIPELINE,
    TASK,
    ANALYST,
    TASK_ERROR,
    PROJECT,
    DATASOURCE,
    PROJECT_STORAGE,
    SYSTEM_STORAGE,
    PIPELINE_STORAGE,
    PIPELINE_MODULE,
    API_KEY,
    CREDENTIALS,
    DATASET,
    CLUSTER_SNAPSHOT,
    CLUSTER_SNAPSHOT_POLICY,
    CLUSTER_REPOSITORY,
    DEPEND,
    MODEL,
    AUTOML,
    FIELD,
    CLIP
}

/**
 * EventLogConfiguration is a Configuration class which is just a method
 * of populating a MeterRegistryHolder with a MeterRegistry instance at startup.
 */
@Configuration
class EventLogConfiguration @Autowired constructor(meterRegistrty: MeterRegistry) {
    init {
        MeterRegistryHolder.meterRegistry = meterRegistrty
    }
}

/**
 * A singleton for storing a MeterRegistry which is accessible from static functions.
 */
object MeterRegistryHolder {

    lateinit var meterRegistry: MeterRegistry

    fun getTags(vararg tags: Tag): MutableList<Tag> {
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
        meterRegistry.counter(name.toLowerCase(), getTags(*tags)).increment()
    }

    /**
     * Get a counter for the metric, the counter will be created if it doesn't already exist.
     *
     * @param name: Name of the counter
     * @return A counter counter that can be incremented
     */
    fun counter(name: String): Counter = meterRegistry.counter(name)
}

/**
 * Format a log message with key value pairs
 *
 * @param obj: The object in question
 * @param action: The object we've took or are about to take
 * @param kvp: A variable arg list of maps which contain additional key/value pairs to log.
 * @return A formatted log string
 */
fun formatLogMessage(obj: LogObject, action: LogAction, vararg kvp: Map<String, Any?>?): String {
    val user = getZmlpActorOrNull()
    val sb = StringBuilder(256)

    sb.append("ZMLPEVENT zmlp.object='${obj.toString().toLowerCase()}' zmlp.action='${action.toString().toLowerCase()}'")
    if (user != null) {
        sb.append(" zmlp.authedProjectId='${user.projectId}' zmlp.actor='${user.name}'")
    }
    kvp?.forEach { e ->
        e?.forEach {
            if (it.value != null) {
                if (it.value is Number || it.value is Boolean) {
                    sb.append(" zmlp.${it.key}=${it.value}")
                } else {
                    sb.append(" zmlp.${it.key}='${it.value}'")
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
    MeterRegistryHolder.increment("zmlp.event.$obj.$action", Tag.of("state", "success"))
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
fun Logger.warnEvent(obj: LogObject, action: LogAction, message: String, kvp: Map<String, Any?>? = null, ex: Exception? = null) {
    // Don't ever pass kvp into MeterRegistryHolder for tags
    MeterRegistryHolder.increment("zmlp.event.$obj.$action", Tag.of("state", "warn"))
    if (this.isWarnEnabled) {
        this.warn(formatLogMessage(obj, action, kvp, kotlin.collections.mapOf("message" to message)), ex)
    }
}
