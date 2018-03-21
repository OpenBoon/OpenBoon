package com.zorroa.archivist.domain

import java.util.*

interface BlobId {
    fun getBlobId(): UUID
}

data class BlobSpec (
        val data: Any,
        val acl: Acl?
)

class Blob (
    private val blobId: UUID,
    val version: Long,
    val app: String,
    val feature: String,
    val name: String,
    val data: Any) : BlobId {

    override fun getBlobId(): UUID {
        return blobId
    }

    val path: String
        get() = arrayOf(app, feature, name).joinToString("/")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Blob
        return Objects.equals(blobId, other.blobId)
    }

    override fun hashCode(): Int {
        return blobId.hashCode()
    }

}
