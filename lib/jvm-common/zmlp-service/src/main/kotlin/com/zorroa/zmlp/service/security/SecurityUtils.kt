package com.zorroa.zmlp.service.security

import com.zorroa.zmlp.apikey.ZmlpActor
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

fun getAuthentication(): Authentication? {
    return SecurityContextHolder.getContext().authentication
}

fun getZmlpActor(): ZmlpActor {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw SecurityException("No credentials")
    } else {
        try {
            auth.principal as ZmlpActor
        } catch (e: java.lang.ClassCastException) {
            throw SecurityException("Invalid credentials", e)
        }
    }
}

fun getZmlpActorOrNull(): ZmlpActor? {
    return try {
        getZmlpActor()
    } catch (ex: Exception) {
        null
    }
}

fun getProjectId(): UUID {
    return getZmlpActor().projectId
}