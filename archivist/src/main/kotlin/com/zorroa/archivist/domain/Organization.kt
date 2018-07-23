package com.zorroa.archivist.domain

import java.util.*

data class OrganizationSpec(
        val name : String
)

data class Organization(
        val id: UUID,
        val name: String
)
