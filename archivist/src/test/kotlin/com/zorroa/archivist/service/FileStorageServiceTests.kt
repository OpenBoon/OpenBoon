package com.zorroa.archivist.service

import com.google.cloud.storage.HttpMethod
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
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultGcsDirectoryLayoutProviderTests : AbstractTest() {

    @Test
    fun testGenerateId() {
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "100", "y", "100"))

        val id = DefaultGcsDirectoryLayoutProvider("foo").buildId(spec)
        assertEquals("proxy___f920837e-9d15-5963-998b-38bd44b17468___so_urgent_x_100_y_100.jpg", id)
    }

    @Test
    fun testGenerateIdNoVariants() {
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg")

        val id = DefaultGcsDirectoryLayoutProvider("foo").buildId(spec)
        assertEquals("proxy___f920837e-9d15-5963-998b-38bd44b17468___so_urgent.jpg", id)
    }

    @Test
    fun testGetUri() {
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg")

        val id1 = DefaultGcsDirectoryLayoutProvider("foo").buildId(spec)
        val uri = DefaultGcsDirectoryLayoutProvider("foo").buildUri(id1)
        println(id1)
        println(uri)
    }

    @Test
    fun testUseAssetId() {
        val assetId = UUID.fromString("16C13597-CE2F-4CB2-8EB0-F01AEA1702D0")
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "200", "y", "200"),assetId=assetId)

        val id = DefaultGcsDirectoryLayoutProvider("foo").buildId(spec)
        assertEquals("proxy___16c13597-ce2f-4cb2-8eb0-f01aea1702d0___so_urgent_x_200_y_200.jpg", id)
    }

    @Test
    fun getUri() {
        val assetId = UUID.fromString("16C13597-CE2F-4CB2-8EB0-F01AEA1702D0")
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "200", "y", "200"),assetId=assetId)

        val uri = DefaultGcsDirectoryLayoutProvider("foo").buildUri(spec)
        println(uri)
    }

    @Test
    fun getIdFromPath() {
        val provider = DefaultGcsDirectoryLayoutProvider("foo")
        val assetId = UUID.fromString("16C13597-CE2F-4CB2-8EB0-F01AEA1702D0")
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "200", "y", "200"),assetId=assetId)

        val uri = provider.buildUri(spec)

        println(uri)
    }

}

class GcsFileStorageServiceTests : AbstractTest() {

    val bucketName = "rmaas-dit-2-zorroa-data"
    val fileStorage: GcsFileStorageService = GcsFileStorageService(bucketName,
            Paths.get("unittest/config/data-credentials.json"))

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
    fun testSignUrl() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val id = fileStorage.dlp.buildId(spec)
        println(fileStorage.getSignedUrl(id, HttpMethod.PUT))
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
}