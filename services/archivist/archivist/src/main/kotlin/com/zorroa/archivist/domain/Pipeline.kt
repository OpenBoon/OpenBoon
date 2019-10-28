package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class PipelineType {
    Import,
    Export,
    Batch,
    Generate
}

@ApiModel("Pipeline", description = "Describes a list of Processors for a Job to run in serial.")
data class Pipeline(

    @ApiModelProperty("UUID of the Pipeline.")
    var id: UUID,

    @ApiModelProperty("Name of the Pipeline.")
    var name: String,

    @ApiModelProperty("Type of the Pipeline.")
    var type: PipelineType,

    @ApiModelProperty("List of processors in this Pipeline.")
    var processors: List<ProcessorRef> = mutableListOf()

)

@ApiModel("Pipeline Spec", description = "Attributes required to create a Pipeline.")
data class PipelineSpec(

    @ApiModelProperty("Name of the Pipeline.")
    var name: String,

    @ApiModelProperty("Type of the Pipeline.")
    var type: PipelineType,

    @ApiModelProperty("Description of the Pipeline.")
    var description: String,

    @ApiModelProperty("List of the Pipeline's processors.")
    var processors: List<ProcessorRef> = mutableListOf()

)
