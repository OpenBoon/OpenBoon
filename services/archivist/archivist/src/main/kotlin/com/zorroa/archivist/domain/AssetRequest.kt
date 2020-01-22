package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.web.multipart.MultipartFile

@ApiModel("Update Asset Request", description = "Request structure to update an Asset.")
class UpdateAssetsByQueryRequest(

    @ApiModelProperty("A script to run on the doc")
    val query: Map<String, Any>? = null,

    @ApiModelProperty("A script to run on the doc")
    val script: Map<String, Any>? = null
)

@ApiModel("Update Asset Request", description = "Request structure to update an Asset.")
class UpdateAssetRequest(

    @ApiModelProperty("Key/value pairs to be updated.")
    val doc: Map<String, Any>? = null,

    @ApiModelProperty("A script to run on the doc")
    val script: Map<String, Any>? = null
)

@ApiModel(
    "Batch Upload Assets Request",
    description = "Defines the properties required to batch upload a list of assets."
)
class BatchUploadAssetsRequest(

    @ApiModelProperty("A list of AssetSpec objects which define the Assets starting metadata.")
    var assets: List<AssetSpec>,

    @ApiModelProperty(
        "Set to true if the assets should undergo " +
            "further analysis, or false to stay in the provisioned state."
    )
    val analyze: Boolean = true,

    @ApiModelProperty("The pipeline to utilize, otherwise use the default Pipeline")
    val pipeline: String? = null,

    @ApiModelProperty("The pipeline modules to execute if any, otherwise utilize the default Pipeline.")
    val modules: List<String>? = null

) {

    lateinit var files: Array<MultipartFile>
}

@ApiModel("Batch Create Assets Request",
    description = "Defines the properties necessary to provision a batch of assets.")
class BatchCreateAssetsRequest(

    @ApiModelProperty("The list of assets to be created")
    val assets: List<AssetSpec>,

    @ApiModelProperty("Set to true if the assets should undergo " +
        "further analysis, or false to stay in the provisioned state.")
    val analyze: Boolean = true,

    @ApiModelProperty("The pipeline to execute, defaults to the project's default pipeline.")
    val pipeline: String? = null,

    @ApiModelProperty("The pipeline modules to execute if any, otherwise utilize the default Pipeline.")
    val modules: List<String>? = null,

    @JsonIgnore
    @ApiModelProperty("The taskId that is creating the assets via expand.", hidden = true)
    val task: InternalTask? = null
)
