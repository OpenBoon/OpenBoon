package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import kotlin.test.assertEquals

class ImageServiceTests : AbstractTest() {

    @Autowired
    lateinit var imageService: ImageService

    @Test
    fun testGetImageDimension() {
        val dim = imageService.getImageDimension(File("src/test/resources/test-data/toucan.jpg"))
        assertEquals(512, dim.width)
        assertEquals(341, dim.height)
    }
}