package com.zorroa.auth.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

fun getProjectId() : UUID {
    val auth = SecurityContextHolder.getContext().authentication
    return auth.principal as UUID
}