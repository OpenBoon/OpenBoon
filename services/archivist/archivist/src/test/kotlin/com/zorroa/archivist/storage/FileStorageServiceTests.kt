package com.zorroa.archivist.storage

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.FileCategory
import com.zorroa.archivist.domain.FileGroup
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.ProjectFileLocator
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class FileStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @Test
    fun testStore() {
        val loc = ProjectFileLocator(FileGroup.ASSET, "1234", FileCategory.SOURCE, "bob.jpg")
        val spec = FileStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())

        val result = fileStorageService.store(spec)
        assertEquals(result.category, "source")
        assertEquals(result.name, "bob.jpg")
        assertEquals(result.size, 4)
        assertEquals(result.mimetype, "image/jpeg")
        assertEquals(result.attrs, mapOf("cats" to 100))
    }
}