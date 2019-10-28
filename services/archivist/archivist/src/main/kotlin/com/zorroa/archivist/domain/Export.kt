package com.zorroa.archivist.domain

import com.zorroa.archivist.search.AssetSearch
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Export File Spec", description = "Defines fields needed to make new Export File.")
data class ExportFileSpec(

    @ApiModelProperty("UUID of the Storage.")
    var storageId: String,

    @ApiModelProperty("Filename the server will set when downloaded.")
    var filename: String

)

/**
 * An ExportFile record.
 */
data class ExportFile(
    val id: UUID,
    val exportId: UUID,
    val name: String,
    val path: String,
    val mimeType: String,
    val size: Long,
    val timeCreated: Long
)

@ApiModel("Export Spec", description = "Defines fields needed to create a new export.")
data class ExportSpec(

    @ApiModelProperty("Name of the Export.")
    var name: String?,

    @ApiModelProperty("Assets from this search filter will get added to the Export.")
    var search: AssetSearch,

    @ApiModelProperty("List of processors to use when processing the Export.")
    var processors: List<ProcessorRef> = mutableListOf(),

    @ApiModelProperty("Args to send to the processors.")
    var args: Map<String, Any> = mutableMapOf(),

    @ApiModelProperty("Environment to use when processing the Export")
    var env: Map<String, String> = mutableMapOf()

)
