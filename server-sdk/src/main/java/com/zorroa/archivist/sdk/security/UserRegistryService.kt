package com.zorroa.archivist.sdk.security

interface UserRegistryService {

    fun registerUser(username: String, source: String, groups: List<String>?) : UserAuthed

    fun getUser(username: String) : UserAuthed
}

