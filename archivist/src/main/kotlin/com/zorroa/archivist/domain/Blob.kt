package com.zorroa.archivist.domain

import java.util.*

interface BlobId {
    fun getBlobId(): Int
}

class Blob : BlobId {

    private var blobId: Int = 0
    private var version: Long = 0

    private var app: String? = null
    private var feature: String? = null
    private var name: String? = null

    private var data: Any? = null

    val path: String
        get() = arrayOf(getApp(), getFeature(), getName()).joinToString("/")

    fun getApp(): String? {
        return app
    }

    fun setApp(app: String): Blob {
        this.app = app
        return this
    }

    fun getFeature(): String? {
        return feature
    }

    fun setFeature(feature: String): Blob {
        this.feature = feature
        return this
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String): Blob {
        this.name = name
        return this
    }

    fun getData(): Any? {
        return data
    }

    fun setData(data: Any): Blob {
        this.data = data
        return this
    }

    override fun getBlobId(): Int {
        return blobId
    }

    fun setBlobId(blobId: Int): Blob {
        this.blobId = blobId
        return this
    }

    fun getVersion(): Long {
        return version
    }

    fun setVersion(version: Long): Blob {
        this.version = version
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val blob = o as Blob?
        return getBlobId() == blob!!.getBlobId()
    }

    override fun hashCode(): Int {
        return Objects.hash(getBlobId())
    }
}
