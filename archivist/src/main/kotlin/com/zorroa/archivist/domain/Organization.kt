package com.zorroa.archivist.domain

import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Organization Spec", description = "Attributes required to create an Organization.")
class OrganizationSpec(

    @ApiModelProperty("Name of the Organization.")
    val name: String,

    @ApiModelProperty("UUID of the ES index route.")
    var indexRouteId: UUID? = null

)

@ApiModel("Organization", description = "Holds all of the assets, user, folders etc and is the top level " +
    "multi-tenant entity.")
class Organization(

    @ApiModelProperty("UUID of the Organization")
    val id: UUID,

    @ApiModelProperty("UUID of the ES cluster this Organization lives on.")
    val indexRouteId: UUID,

    @ApiModelProperty("Name of the Organization.")
    val name: String

) {
    companion object {
        val DEFAULT_ORG_ID = UUID.fromString("00000000-9998-8888-7777-666666666666")
    }
}

@ApiModel("Organization Update Spec", description = "Attributes for updating an Organization.")
class OrganizationUpdateSpec(

    @ApiModelProperty("Name of the Organization.")
    var name: String,

    @ApiModelProperty("Will update the Organizations's ES cluster address. Will not move files.")
    var indexRouteId: UUID

)

@ApiModel("Organization Filter", description = "Filter for finding Organizations.")
class OrganizationFilter(

    @ApiModelProperty("Organization UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Names to match.")
    val names: List<String>? = null,

    @ApiModelProperty("Index route UUIDs to match.")
    val indexRouteIds: List<UUID>? = null

) : KDaoFilter() {

    override val sortMap: Map<String, String> = mapOf(
            "name" to "organization.str_name",
            "id" to "organization.pk_organization",
            "indexRouteId" to "organization.pk_index_route")

    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:a")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("organization.pk_organization", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("organization.str_name", it.size))
            addToValues(it)
        }

        indexRouteIds?.let {
            addToWhere(JdbcUtils.inClause("organization.pk_index_route", it.size))
            addToValues(it)
        }
    }
}

/**
 * A simple class for determining the dispatch priority of organization.
 *
 * @property organizationId The Organization Id.
 * @property priority The priority of the Organization, lower is higher priority.
 */
class DispatchPriority(
    val organizationId: UUID,
    val priority: Int
)
