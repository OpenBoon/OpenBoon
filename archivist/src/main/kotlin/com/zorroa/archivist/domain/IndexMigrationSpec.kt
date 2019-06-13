package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * An IndexMigrationSpec allows users to migrate data from one index route to another.  Both
 * route's have to to exist.
 */
@ApiModel("IndexMigrationSpec", description = "Describes an ES index migration")
class IndexMigrationSpec(

    @ApiModelProperty("The desination IndexRoute id.")
    val dstRouteId: UUID,

    @ApiModelProperty("True if the organization's route should swap before reindexing.")
    val swapRoutes: Boolean = true,

    @ApiModelProperty("An array of attributes to remove during reindexing")
    val removeAttrs: List<String>? = null,

    @ApiModelProperty("An array of attributes to set during reindexing")
    val setAttrs: Map<String, Any>? = null
)
