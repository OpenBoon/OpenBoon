package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.UUIDFileSystem
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.util.Json

import org.junit.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GcsFileStorageServiceTests : AbstractTest() {

    val bucketName = "zorroa-test"
    val fileStorage: GcsFileStorageService = GcsFileStorageService(bucketOverride = "zorroa-test")

    @Test
    fun testGetUri() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val uri = fileStorage.dlp.buildUri(spec)

        val expectedPath = "/orgs/00000000-9998-8888-7777-666666666666/ofs/proxy/38f154c7-e214-551b-9374-075c2bac63a1/foo_bar.jpg"
        val auri = URI.create(uri)

        assertEquals(bucketName,  auri.authority)
        assertEquals(expectedPath, auri.path)
        assertEquals("gs", auri.scheme)
    }

    @Test
    fun testGetId() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val id = fileStorage.dlp.buildId(spec)
        assertTrue(id.startsWith("proxy_"))
        assertTrue(id.endsWith("_foo_bar.jpg"))
    }

    @Test
    fun testCreate() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val storage = fileStorage.create(spec)
        assertEquals(storage.mimeType, "image/jpeg")
    }

    @Test
    fun testGetGetBySpec() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val storage = fileStorage.create(spec)
        assertEquals(storage.mimeType, "image/jpeg")
    }

    @Test
    fun testGetGetById() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val storage1 = fileStorage.create(spec)
        val storage2 = fileStorage.get(storage1.id)
        assertEquals(storage1.uri, storage2.uri)
        assertEquals(storage1.id, storage2.id)
        assertEquals(storage1.mimeType, storage2.mimeType)
    }

}

class LocalFileStorageServiceTests : AbstractTest() {

    val testShared = Files.createTempDirectory("test")
    val ofs: UUIDFileSystem = UUIDFileSystem(testShared.resolve("ofs").toFile())
    val fileStorage: LocalFileStorageService

    init {
        ofs.init()
        fileStorage = LocalFileStorageService(testShared, ofs)
    }

    @Test
    fun testCreateWithVariants() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg",
                listOf("x", "100", "y", "100"))
        val fs = fileStorage.create(spec)
        assertTrue(FileUtils.filename(fs.uri).endsWith("x_100_y_100.jpg"))
        assertEquals("image/jpeg", fs.mimeType)
    }


    @Test
    fun testGetById() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg",
                listOf("x", "100", "y", "100"))
        val fs = fileStorage.create(spec)
        println(fs.id)
    }



    @Test
    fun testCreate() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg", null)
        val fs = fileStorage.create(spec)
        assertTrue(FileUtils.filename(fs.uri).endsWith(".jpg"))
        assertEquals("image/jpeg", fs.mimeType)
        assertEquals("file", fs.scheme)
    }

    @Test
    fun testStatNotExists() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg",
                listOf("x", "100", "y", "100"))
        val stat = fileStorage.get(spec)
        assertFalse(stat.exists)
        assertEquals("image/jpeg", stat.mimeType)
    }

    @Test
    fun testStatExists() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg", listOf("x", "100", "y", "100"))
        val fs = fileStorage.create(spec)
        Files.copy(getTestImagePath().resolve("beer_kettle_01.jpg"),
                Paths.get(URI(fs.uri)), StandardCopyOption.REPLACE_EXISTING)
        val stat = fileStorage.get(spec)
        assertTrue(stat.exists)
        assertEquals("image/jpeg", stat.mimeType)
        assertEquals(1800475, stat.size)
    }
}