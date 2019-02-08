package com.zorroa.archivist.domain

import com.fasterxml.jackson.core.type.TypeReference

fun zpsTaskName(zps: ZpsScript) : String {
    if (zps.name == null) {
        val list = mutableListOf<String>()

        zps.generate?.let {
            list.add("Generators=${it.size}")
        }

        zps.execute?.let {
            list.add("Processors=${it.size}")
        }

        zps.over?.let {
            list.add("Assets=${it.size}" )
        }
        return list.joinToString(" ")
    }
    else {
        return zps.name!!
    }
}

fun emptyZpsScript(name: String) : ZpsScript {
    return ZpsScript(name,
            null, null, null)
}


class ZpsScript(
        var name: String?,
        var generate : List<ProcessorRef>?,
        var over: List<Document>?,
        var execute : List<ProcessorRef>?,
        var globals:  MutableMap<String, Any>? = mutableMapOf(),
        var type: PipelineType = PipelineType.Import,
        var settings: MutableMap<String, Any>?=null
)
{
    /**
     * Set a key/value in the settings map.  If the settings map is
     * null then one is created.
     *
     * @param key: The name of the setting
     * @param value: value for the setting.
     */
    fun setSettting(key: String, value: Any) {
        if (settings == null) {
            settings = mutableMapOf()
        }
        settings?.let {
            it[key] = value
        }
    }

    /**
     * Set a key/value in the global arg map.  If the arg map is
     * null then one is created.
     *
     * @param key: The name of the arg
     * @param value: value for the arg.
     */
    fun setGlobalArg(key: String, value: Any) {
        if (globals == null) {
            globals = mutableMapOf()
        }
        globals?.let {
            it[key] = value
        }
    }
}

class ZpsError (
        var id: String? = null,
        var path: String? = null,
        var phase: String? = null,
        var message: String? = null,
        var processor: String? = null,
        var className: String? = null,
        var file: String? = null,
        var lineNumber: Int? = null,
        var method: String? = null,
        var skipped : Boolean = false)


class ZpsFilter(
        var expr: String? = null,
        var drop : Boolean = false
)

class ProcessorRef(
        var className: String,
        var args: Map<String, Any>? = mutableMapOf(),
        var execute: List<ProcessorRef>? = mutableListOf(),
        var filters: List<ZpsFilter>? = mutableListOf(),
        var fileTypes: Set<String>? = mutableSetOf(),
        val language : String = "python"
)

var LIST_OF_PREFS: TypeReference<List<ProcessorRef>> = object : TypeReference<List<ProcessorRef>>() {}