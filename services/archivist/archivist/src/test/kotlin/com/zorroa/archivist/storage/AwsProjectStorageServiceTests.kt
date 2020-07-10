package com.zorroa.archivist.storage

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * This is just testing the AWS implementation.
 */
class AwsProjectStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Test
    fun testStore() {
        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            "1234", ProjectStorageCategory.SOURCE, "bob.jpg"
        )
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = projectStorageService.store(spec)
        assertEquals(result.category, "source")
        assertEquals(result.name, "bob.jpg")
        assertEquals(result.size, 4)
        assertEquals(result.mimetype, "image/jpeg")
        assertEquals(result.attrs, mapOf("cats" to 100))
    }

    @Test(expected = ProjectStorageException::class)
    fun testDelete() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)
        projectStorageService.recursiveDelete(
            ProjectDirLocator(ProjectStorageEntity.ASSETS, loc.entityId)
        )
        // Throws ProjectStorageException
        projectStorageService.fetch(loc)
    }

    @Test
    fun testFetch() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = projectStorageService.store(spec)
        val bytes = projectStorageService.fetch(loc)
        assertEquals(result.size, bytes.size.toLong())
    }

    @Test
    fun testStream() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)

        val entity = projectStorageService.stream(loc)
        val value = String(entity.body.inputStream.readBytes())
        assertEquals("test", value)
    }

    @Test
    fun testGetSignedUrl() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val rsp = projectStorageService.getSignedUrl(loc, true, 60, TimeUnit.MINUTES)
        Json.prettyPrint(rsp)
    }

    @Test
    fun testNotAlphaNumericFileName() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob test.txt")

        var expected = "s3://project-storage-test/projects/00000000-0000-0000-0000-000000000000/assets/1234/source/${URLEncoder.encode("bob test.txt", StandardCharsets.UTF_8.toString())}"

        val rsp = projectStorageService.getNativeUri(loc)
        Json.prettyPrint(rsp)
        assertEquals(expected, rsp)
    }

    @Test
    fun testEncodedLocation() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob%20test.txt")

        var expected = "s3://project-storage-test/projects/00000000-0000-0000-0000-000000000000/assets/1234/source/${URLEncoder.encode("bob test.txt", StandardCharsets.UTF_8.toString())}"

        val rsp = projectStorageService.getNativeUri(loc)
        Json.prettyPrint(rsp)
        assertEquals(expected, rsp)
    }
}
