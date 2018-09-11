package com.zorroa.archivist.domain

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
    var processors: List<ProcessorRef> = mutableListOf()
)

data class PipelineSpec (
        var name: String,
        var type: PipelineType,
        var description: String,
        var processors: List<ProcessorRef> = mutableListOf()
)

