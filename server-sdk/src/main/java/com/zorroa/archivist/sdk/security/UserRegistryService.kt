package com.zorroa.archivist.sdk.security

data class AuthSource(val label:String,
                      val authSourceId:String,
                      val permissionType:String)

interface UserRegistryService {

    fun registerUser(username: String, source: AuthSource, groups: List<String>?) : UserAuthed

    fun getUser(username: String) : UserAuthed
}

