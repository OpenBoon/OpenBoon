package com.zorroa.archivist.storage

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

/**
 * This is just testing the AWS implementation.
 */
class AwsProjectStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Test
    fun testStore() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSET,
            "1234", ProjectStorageCategory.SOURCE, "bob.jpg")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = projectStorageService.store(spec)
        assertEquals(result.category, "source")
        assertEquals(result.name, "bob.jpg")
        assertEquals(result.size, 4)
        assertEquals(result.mimetype, "image/jpeg")
        assertEquals(result.attrs, mapOf("cats" to 100))
        projectStorageService.deleteAsset(loc.entityId)
    }

    @Test(expected = AmazonS3Exception::class)
    fun testDelete() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSET, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)
        projectStorageService.deleteAsset(loc.entityId)

        // Throws AmazonS3Exception
        projectStorageService.fetch(loc)
    }

    @Test
    fun testFetch() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSET, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = projectStorageService.store(spec)
        val bytes = projectStorageService.fetch(loc)
        assertEquals(result.size, bytes.size.toLong())
        projectStorageService.deleteAsset(loc.entityId)
    }

    @Test
    fun testStream() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSET, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)

        val entity = projectStorageService.stream(loc)
        val value = String(entity.body.inputStream.readBytes())
        assertEquals("test", value)
        projectStorageService.deleteAsset(loc.entityId)
    }
}
