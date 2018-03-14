package com.zorroa.security

interface UserRegistryService {

    fun registerUser(username: String, source: String, groups: List<String>?) : UserAuthed

    fun getUser(username: String) : UserAuthed
}

