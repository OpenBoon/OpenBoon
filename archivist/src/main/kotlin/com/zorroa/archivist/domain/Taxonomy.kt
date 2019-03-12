package com.zorroa.archivist.domain

import java.util.*

/**
 * A Taxonomy turns a folder structure into an auto-tagging system.
 *
 * @property taxonomyId Rhe ID of the taxonomy.
 * @property folderId The root folder Id of the taxonomy.
 * @property organizationId The organization Id for the taxonomy.
 * @property createdUser The user that created the taxonomy.
 * @property timeCreated The time the taxonomy was created.
 */
class Taxonomy(
        val taxonomyId: UUID,
        val folderId: UUID,
        val organizationId: UUID,
        val createdUser: UUID,
        val timeCreated : Long) {

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