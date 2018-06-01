package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.sdk.services.AssetId
import com.zorroa.archivist.sdk.services.StorageWriteException
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageServiceTests : AbstractTest() {

    lateinit var asset: AssetId

    @Before
    fun init() {
        asset = AssetId(UUID.randomUUID(), UUID.randomUUID(), "foo.bar")
    }

    @Test(expected = StorageWriteException::class)
    fun testRemoveBucketFailure() {
        storageService.removeBucket(asset)
    }

    @Test
    fun testRemoveBucket() {
        storageService.createBucket(asset)
        storageService.removeBucket(asset)
    }

    @Test
    fun testCreateBucket() {
        storageService.createBucket(asset)
        assertTrue(storageService.bucketExists(asset))
    }

    @Test
    fun testStoreAndGetMetadata() {
        val metadata = mapOf("shin" to "dig")
        storageService.createBucket(asset)
        storageService.storeMetadata(asset, metadata)
        val stored = storageService.getMetadata(asset)
        assertEquals(metadata, stored)
    }

    @Test
    fun testStoreAndGetSourceFile() {
        storageService.createBucket(asset)
        storageService.storeSourceFile(asset, FileInputStream(
                getTestImagePath("set01/toucan.jpg").toFile()))
        val stream = storageService.getSourceFile(asset)
        val target = File("/tmp/toucan.jpg")
        FileUtils.copyInputStreamToFile(stream, target)
        assertEquals(Files.size(target.toPath()), Files.size(getTestImagePath("set01/toucan.jpg")))
    }
}
