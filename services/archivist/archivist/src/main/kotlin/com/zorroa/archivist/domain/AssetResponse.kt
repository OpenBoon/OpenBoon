package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Batch Create Assets Response",
    description = "The response returned after provisioning assets.")
class BatchCreateAssetsResponse(

    @ApiModelProperty("The ES bulk response.")
    val bulkResponse: Map<String, Any>,

    @ApiModelProperty("A map of failed asset ids to error message")
    val failed: List<Map<String, String?>>,

    @ApiModelProperty("A list of asset Ids created.")
    val created: List<String>,

    @ApiModelProperty("The ID of the analysis job, if analysis was selected")
    var jobId: UUID? = null
)
