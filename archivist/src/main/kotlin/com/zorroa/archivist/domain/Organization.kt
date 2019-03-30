package com.zorroa.archivist.domain

import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

class OrganizationSpec(
        val name: String
)

class Organization(
        val id: UUID,
        val name: String
) {
    companion object {
        val DEFAULT_ORG_ID = UUID.fromString("00000000-9998-8888-7777-666666666666")
    }
}

class OrganizationUpdateSpec(
        val name: String
)

class OrganizationFilter(
        val ids : List<UUID>?=null,
        val names: List<String>?=null) : KDaoFilter() {

    override val sortMap: Map<String, String> = mapOf(
            "name" to "organization.str_name",
            "id" to "organization.pk_organization")

    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:a")
        }

        ids?.let  {
            addToWhere(JdbcUtils.inClause("organization.pk_organization", it.size))
            addToValues(it)
        }

        names?.let  {
            addToWhere(JdbcUtils.inClause("organization.str_name", it.size))
            addToValues(it)
        }
    }
}
