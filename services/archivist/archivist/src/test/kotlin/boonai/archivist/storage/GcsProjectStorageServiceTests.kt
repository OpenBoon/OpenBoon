package boonai.archivist.storage

import boonai.archivist.AbstractTest
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageSpec
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import java.util.UUID
import kotlin.test.assertEquals

class GcsProjectStorageServiceTests : AbstractTest() {

    @Mock
    lateinit var googleStorageService: GcsProjectStorageService

    @Test
    fun storeTest() {
        whenever(googleStorageService.store(any())).thenReturn(
            FileStorage(
                "1234",
                "test_path",
                "source",
                "image/jpeg",
                4,
                mapOf("cats" to 100)
            )
        )

        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            "1234", ProjectStorageCategory.SOURCE, "bob.jpg", projectId = UUID.randomUUID()
        )
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = googleStorageService.store(spec)
        assertEquals("1234", result.id)
        assertEquals("source", result.category)
        assertEquals("test_path", result.name)
        assertEquals(4, result.size)
        assertEquals("image/jpeg", result.mimetype)
        assertEquals(mapOf("cats" to 100), result.attrs)
    }

    @Test
    fun recursiveDeleteTest() {
        whenever(googleStorageService.recursiveDelete(anyString())).then {}
        whenever(googleStorageService.listFiles(anyString())).thenReturn(emptyList())

        googleStorageService.recursiveDelete("bucket-name/project-id")

        val listOfItens = googleStorageService.listFiles("bucket-name/project-id")

        assert(listOfItens.isEmpty())
    }
}
