package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.UUIDFileSystem
import com.zorroa.archivist.util.FileUtils

import org.junit.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class OfsFileStorageServiceTests : AbstractTest() {

    val ofs: UUIDFileSystem = UUIDFileSystem(Files.createTempDirectory("test").toFile())
    val fileStorage: OfsFileStorageService

    init {
        ofs.init()
        fileStorage = OfsFileStorageService(ofs)
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