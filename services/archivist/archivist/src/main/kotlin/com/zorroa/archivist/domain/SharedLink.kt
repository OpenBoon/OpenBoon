package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Shared Link Spec", description = "defines the properties needed for creating SharedLink.")
class SharedLinkSpec(

    @ApiModelProperty("State of the UI.")
    var state: Map<String, Any>,

    @ApiModelProperty("UUIDs of Users to email.")
    var userIds: Set<UUID>? = null
)

@ApiModel("Shared Link", description = "Points to a specific curator UI state that can be shared via a link.")
class SharedLink(

    @ApiModelProperty("UUID of the Shared Link.")
    val id: UUID,

    @ApiModelProperty("State of the UI.")
    val state: Map<String, Any>,

    @ApiModelProperty("User UUIDs that were notified.")
    val userIds: Set<UUID>? = null

)
