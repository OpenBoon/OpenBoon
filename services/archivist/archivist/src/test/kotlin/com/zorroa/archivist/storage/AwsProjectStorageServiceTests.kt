package com.zorroa.archivist.storage

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.AssetFileLocator
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * This is just testing the AWS implementation.
 */
class AwsProjectStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Test
    fun testStore() {
        val loc = AssetFileLocator("1234", ProjectStorageCategory.SOURCE, "bob.jpg")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = projectStorageService.store(spec)
        assertEquals(result.category, "source")
        assertEquals(result.name, "bob.jpg")
        assertEquals(result.size, 4)
        assertEquals(result.mimetype, "image/jpeg")
        assertEquals(result.attrs, mapOf("cats" to 100))
        projectStorageService.delete(loc)
    }

    @Test
    fun testDelete() {
        val loc = AssetFileLocator("1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)
        projectStorageService.delete(loc)

        assertFailsWith(AmazonS3Exception::class) {
            projectStorageService.fetch(loc)
        }
    }

    @Test
    fun testFetch() {
        val loc = AssetFileLocator("1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = projectStorageService.store(spec)
        val bytes = projectStorageService.fetch(loc)
        assertEquals(result.size, bytes.size.toLong())
        projectStorageService.delete(loc)
    }

    @Test
    fun testStream() {
        val loc = AssetFileLocator("1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)

        val entity = projectStorageService.stream(loc)
        val value = String(entity.body.inputStream.readBytes())
        assertEquals("test", value)
        projectStorageService.delete(loc)
    }
}
