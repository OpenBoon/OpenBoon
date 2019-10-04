package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Dynamic Hierarchy Spec", description = "Data required to create a Dynamic Hierarchy (DyHi).")
class DyHierarchySpec(

    @ApiModelProperty("UUID of the Folder to apply the DyHi to.")
    val folderId: UUID,

    @ApiModelProperty("List of the DyHi Levels.")
    val levels: List<DyHierarchyLevel>,

    @ApiModelProperty("If true the DyHi will be generated.")
    val generate: Boolean = true

)
