package com.zorroa.archivist.sdk.services

import java.io.InputStream
import java.io.OutputStream

fun getBucketName(asset: AssetId) : String {
    return "${asset.organizationId}"
}

fun getFileName(asset: AssetId, name:String?=null) : String {
    val prefix = asset.id.toString().subSequence(0, 4)
    return if (name == null) {
        "$prefix/${asset.id}/${asset.filename}"
    } else {
        "$prefix/${asset.id}/$name"
    }
}

interface StorageService {

    fun createBucket(asset: AssetId)
    fun removeBucket(asset: AssetId)
    fun bucketExists(asset: AssetId) : Boolean

    fun storeMetadata(asset: AssetId, metadata: Map<String, Any>?)
    fun storeSourceFile(asset: AssetId, stream: InputStream)
    fun getSourceFile(asset: AssetId): InputStream
    fun streamSourceFile(asset: AssetId, output: OutputStream)
    fun getMetadata(asset: AssetId): Map<String, Any>

    fun storeFile(asset: AssetId, name: String, stream: InputStream)
    fun getFile(asset: AssetId, name: String): InputStream
    fun streamFile(asset: AssetId, name: String, output: OutputStream)
}

data class Bucket (val name: String)

open class StorageException(e: Exception) : RuntimeException(e)
class StorageWriteException (e: Exception) : StorageException(e)
class StorageReadException (e: Exception) : StorageException(e)
