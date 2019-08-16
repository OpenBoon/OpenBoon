package com.zorroa.archivist.sdk.security

import java.util.UUID

data class AuthSource(
    val label: String,
    val authSourceId: String,
    val permissionType: String,
    var organizationName: String? = null,
    val attrs: Map<String, String> = mapOf(),
    val groups: List<String>? = null,
    val userId: UUID? = null
)

interface UserRegistryService {

    fun registerUser(username: String, source: AuthSource): UserAuthed

    fun getUser(username: String): UserAuthed

    fun getUser(id: UUID): UserAuthed

    fun exists(username: String, source: String?): Boolean

    fun createSessionToken(id: UUID) : String
}
