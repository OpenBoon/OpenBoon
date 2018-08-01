package com.zorroa.archivist.sdk.security

data class AuthSource(val label:String,
                      val authSourceId:String,
                      val permissionType:String,
                      var organizationName: String?=null,
                      val attrs: Map<String, String>?=null,
                      val groups: List<String>?=null)

interface UserRegistryService {

    fun registerUser(username: String, source: AuthSource) : UserAuthed

    fun getUser(username: String) : UserAuthed
}

