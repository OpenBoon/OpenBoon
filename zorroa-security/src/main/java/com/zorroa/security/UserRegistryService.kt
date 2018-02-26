package com.zorroa.security

interface UserRegistryService {

    fun registerUser(username: String, source: String) : UserAuthed

    fun getUser(username: String) : UserAuthed
}

