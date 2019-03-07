package com.zorroa.archivist.domain

import java.util.*

/**
 *     private UUID taxonomyId;
private UUID folderId;
private boolean active;
private long timeStarted;
private long timeStopped;

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