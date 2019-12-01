package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("File Stat.", description = "Stats for file on disk.")
data class FileStat(
    @ApiModelProperty("File size in bytes.") val size: Long,
    @ApiModelProperty("Media type of the file.") val mediaType: String,
    @ApiModelProperty("True if the file exists on disk.") val exists: Boolean
)