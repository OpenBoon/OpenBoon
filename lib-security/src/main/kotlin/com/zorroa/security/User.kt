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
        var organizationId: UUID,
        username: String,
        permissions: Set<out GrantedAuthority>,
        attrs: Map<String,Any>) : UserId, UserDetails, Serializable {

    val attrs : MutableMap<String, Any> = attrs.toMutableMap()
    private val user : String = username
    private val permissions : Set<out GrantedAuthority> = permissions

    fun setAttr(key: String, value: Any?) {
        if (value == null) {
            attrs.remove(key)
        }
        else {
            attrs[key] = value
        }
    }

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
