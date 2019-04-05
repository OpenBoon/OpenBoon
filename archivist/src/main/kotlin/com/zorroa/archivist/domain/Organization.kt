package com.zorroa.archivist.domain

import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

/**
 * All properties needed to create an organization.
 *
 * @property name The name of the origizaiton, must be unique.
 * @property indexRouteId The route to the ES index this org is assigned to.
 * If null, one is selected.
 *
 */
class OrganizationSpec(
        val name: String,
        var indexRouteId: UUID?=null
)

/**
 * An Organization holds all of the assets, user, folders etc and is the top level
 * multi-tenant entity.
 *
 * @property id The unique ID of the Organization
 * @property indexRouteId The unique ID of the ES cluster this Organization lives on.
 * @property name The unique name of the Organization.
 */
class Organization(
        val id: UUID,
        val indexRouteId: UUID,
        val name: String
) {
    companion object {
        val DEFAULT_ORG_ID = UUID.fromString("00000000-9998-8888-7777-666666666666")
    }
}

/**
 * All properties available for updating an organization.
 *
 * @property name Will renames the organization.
 *  @property indexRouteId Will update the Organizations's ES cluster address. Will not move files.
 */
class OrganizationUpdateSpec(
        var name: String,
        var indexRouteId: UUID
)

/**
 * Options available for filtering organizations.
 *
 * @property ids A list of unique organization ids.
 * @property indexRouteIds A list of unique organization names.
 * @property indexRouteIds A list of [IndexRoute] ids.
 */
class OrganizationFilter(
        val ids : List<UUID>?=null,
        val names: List<String>?=null,
        val indexRouteIds: List<UUID>?=null) : KDaoFilter() {

    override val sortMap: Map<String, String> = mapOf(
            "name" to "organization.str_name",
            "id" to "organization.pk_organization",
            "indexRouteId" to "organization.pk_index_route")

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

        indexRouteIds?.let  {
            addToWhere(JdbcUtils.inClause("organization.pk_index_route", it.size))
            addToValues(it)
        }
    }
}
