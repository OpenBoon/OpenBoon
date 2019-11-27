package com.zorroa.auth.security

import com.zorroa.auth.domain.ZmlpActor
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

fun getProjectId(): UUID {
    val auth = SecurityContextHolder.getContext().authentication
    val user = auth.principal as ZmlpActor
    return user.projectId
}