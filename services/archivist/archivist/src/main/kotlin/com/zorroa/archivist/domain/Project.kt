package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * All properties needed to create a Project
 */
@ApiModel("Project Spec", description = "All properties necessary to create a Project.")
class ProjectSpec (

    @ApiModelProperty("A unique name of the project.")
    val name: String,

    /**
     * Allow unittests to specify a project Id. Not allowed
     * for REST clients.
     */
    @ApiModelProperty("An optional unique ID for the project.")
    val projectId: UUID?=null
)

/**
 * Projects represent unique groups of resources provided by ZMLP.
 */
@Entity
@Table(name = "project")
@ApiModel("Project", description = "A ZMLP Project")
class Project (
    @Id
    @Column(name="pk_project")
    @ApiModelProperty("The Unique ID of the project.")
    val id: UUID,

    @Column(name="str_name")
    @ApiModelProperty("The name of the Project")
    val name: String,

    @Column(name="time_created")
    @ApiModelProperty("The time the Project was created..")
    val timeCreated: Long,

    @Column(name="time_modified")
    @ApiModelProperty("The last time the Project was modified.")
    val timeModified: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Project

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel("Project Filter", description = "Search filter for finding Projects")
class ProjectFilter (

    /**
     * A list of unique Project IDs.
     */
    @ApiModelProperty("The Project IDs to match.")
    val ids: List<UUID>? = null,

    /**
     * A list of unique Project names.
     */
    @ApiModelProperty("The project names to match")
    val names: List<String>? = null

) : KDaoFilter()
{
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "str_name",
        "timeCreated" to "time_created",
        "timeModified" to "time_modified",
        "id" to "pk_project")

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("project.pk_project", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("project.str_name", it.size))
            addToValues(it)
        }
    }
}