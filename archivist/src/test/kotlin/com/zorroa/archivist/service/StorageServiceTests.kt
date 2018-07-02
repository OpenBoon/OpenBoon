package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.Asset
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Files
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ignoring all these for now, most GCP methods are not enabled and actually
 * can probably be deleted.
 */
@Ignore
class StorageServiceTests : AbstractTest() {

    lateinit var asset: Asset

    @Before
    fun init() {
        asset = Asset(UUID.randomUUID(), UUID.randomUUID())
    }

    @Test
    fun testGetObjectStream() {
        val bs = storageService.getObjectStream(
                URL("https://storage.cloud.google.com/rmaas-us-dit1/100/ef0ca910-5c67-4b03-bb8e-c92e2d7e7283/file/proxy_ef0ca910-5c67-4b03-bb8e-c92e2d7e7283_197x256.jpg"))
        logger.info(bs.type)
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
