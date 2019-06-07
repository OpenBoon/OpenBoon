package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Taxonomy", description = "Taxonomy turns a folder structure into an auto-tagging system.")
class Taxonomy(

    @ApiModelProperty("UUID of the taxonomy.")
    val taxonomyId: UUID,

    @ApiModelProperty("UUID of the root Folder of the Taxonomy.")
    val folderId: UUID,

    @ApiModelProperty("UUID of the Organization.")
    val organizationId: UUID,

    @ApiModelProperty("User that created the Taxonomy.")
    val createdUser: UUID,

    @ApiModelProperty("Time the Taxonomy was created.")
    val timeCreated: Long

) {

    val clusterLockId = "taxi-$taxonomyId"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Taxonomy

        if (taxonomyId != other.taxonomyId) return false

        return true
    }

    override fun hashCode(): Int {
        return taxonomyId.hashCode()
    }
}

/**
 * The result of tagging a [Taxonomy]
 *
 * @param assetCount The number of assets tagged.
 * @param folderCount The number of folders processed.
 * @param timestamp The new timetamp for the [Taxonomy]
 *
 */
class TagTaxonomyResult(
    val assetCount: Long,
    val folderCount: Long,
    val timestamp: Long
)