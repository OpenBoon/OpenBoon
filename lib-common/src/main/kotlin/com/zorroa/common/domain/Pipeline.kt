package com.zorroa.common.domain

import java.util.*

data class Pipeline (
    val id: UUID,
    var name: String,
    var version: Long = 1,
    var processors: List<ProcessorRef> = mutableListOf()
)

data class PipelineSpec (
        val name: String,
        val version: Long = 1,
        val processors: List<ProcessorRef> = mutableListOf()
)

