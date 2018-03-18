package com.zorroa.archivist.sdk.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable
import java.util.*

interface UserId {
    val id : UUID

    fun getName() : String
}

class UserAuthed(
        override val id: UUID,
        username: String,
        permissions: Set<out GrantedAuthority>) : UserId, UserDetails, Serializable {

    private val user : String = username
    private val permissions : Set<out GrantedAuthority> = permissions

    override fun getAuthorities(): Collection<out GrantedAuthority> {
        return permissions
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun getUsername(): String {
        return user
    }

    override fun getName(): String {
        return user
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun getPassword(): String {
        return "<HIDDEN>"
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }
}
