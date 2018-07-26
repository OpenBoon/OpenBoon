package com.zorroa.common.domain


data class ZpsScript(
        val name: String,
        val generate : MutableList<ProcessorRef>? = mutableListOf(),
        val over: MutableList<Document>? =  mutableListOf(),
        val execute : MutableList<ProcessorRef>? = mutableListOf(),
        val globals:  MutableMap<String, Any>? = mutableMapOf(),
        var inline: Boolean = true
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


data class ProcessorFilter(
        var expr: String? = null,
        var drop : Boolean = false
)

data class ProcessorRef(
        var className: String,
        var args: Map<String, Any>? = mutableMapOf(),
        var execute: List<ProcessorRef>? = mutableListOf(),
        var filters: List<ProcessorFilter>? = mutableListOf(),
        var fileTypes: Set<String>? = mutableSetOf(),
        val language : String = "python"
)
