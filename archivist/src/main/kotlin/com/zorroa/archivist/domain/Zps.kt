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


data class ZpsScript(
        var name: String?,
        var generate : List<ProcessorRef>?,
        var over: List<Document>?,
        var execute : List<ProcessorRef>?,
        var globals:  MutableMap<String, Any>? = mutableMapOf(),
        var inline: Boolean = true,
        var type: PipelineType = PipelineType.Import,
        var settings: Map<String,Any>?=null
)

data class ZpsError (
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


data class ZpsFilter(
        var expr: String? = null,
        var drop : Boolean = false
)

data class ProcessorRef(
        var className: String,
        var args: Map<String, Any>? = mutableMapOf(),
        var execute: List<ProcessorRef>? = mutableListOf(),
        var filters: List<ZpsFilter>? = mutableListOf(),
        var fileTypes: Set<String>? = mutableSetOf(),
        val language : String = "python"
)

var LIST_OF_PREFS: TypeReference<List<ProcessorRef>> = object : TypeReference<List<ProcessorRef>>() {}