package com.zorroa.common.domain

import java.util.*

enum class PipelineType {
    Import,
    Export,
    Batch,
    Generate
}

data class Pipeline (
    var id: UUID,
    var name: String,
    var type: PipelineType,
    var version: Long = 1,
    var processors: List<ProcessorRef> = mutableListOf()
)

data class PipelineSpec (
        val name: String,
        var type: PipelineType,
        val version: Long = 1,
        val processors: List<ProcessorRef> = mutableListOf()
)

