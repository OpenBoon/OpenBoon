package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * The type of request.  Only export is supported currently.
 */
enum class RequestType {
    Export,
    Online,
    Offline,
    Remove
}

/**
 * Here as a place holder.
 */
enum class RequestState {
    Submitted,
    InProgress,
    Approved,
    Denied
}

@ApiModel("Request Spec", description = "Attributes required to create a Request.")
data class RequestSpec(

    @ApiModelProperty("UUID of the Folder being request for export.")
    val folderId: UUID,

    @ApiModelProperty("Type of this Request.")
    val type: RequestType,

    @ApiModelProperty("Comment about this request.")
    val comment: String = "",

    @ApiModelProperty("Email addresses to CC on this Request.")
    val emailCC: List<String> = listOf()

)

@ApiModel("Request", description = "Describes a user request to export a Folder.")
data class Request(

    @ApiModelProperty("UUID of the Request.")
    val id: UUID,

    @ApiModelProperty("UUID of the Folder to export.")
    val folderId: UUID,

    @ApiModelProperty("Type of the Request.")
    val type: RequestType,

    @ApiModelProperty("User that created the Request.")
    val userCreated: UserBase,

    @ApiModelProperty("Time the Request was created.")
    val timeCreated: Long,

    @ApiModelProperty("User that last modified the Request.")
    val userModified: UserBase,

    @ApiModelProperty("Time the Request was last modified.")
    val timeModified: Long,

    @ApiModelProperty("Current state of the Request.")
    val state: RequestState,

    @ApiModelProperty("Comment about the Request.")
    val comment: String,

    @ApiModelProperty("List of email addresses to CC on this Request.")
    val emailCC: List<String>

)
