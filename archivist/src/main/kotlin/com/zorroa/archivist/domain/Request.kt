package com.zorroa.archivist.domain

import java.util.*

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

class RequestSpec {
    var folderId: UUID? = null
    var type : RequestType? = null
    var comment : String = ""
    var emailCC : List<String> = listOf()
}

data class Request (
        val id : UUID,
        val folderId: UUID,
        val type : RequestType,
        val userCreated : UserBase,
        val timeCreated : Long,
        val userModified : UserBase,
        val timeModified : Long,
        val state: RequestState,
        val comment: String,
        val emailCC: List<String>
)
