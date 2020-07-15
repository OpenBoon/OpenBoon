package com.zorroa.archivist.storage

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Clip
import com.zorroa.archivist.util.bd
import com.zorroa.zmlp.service.storage.SystemStorageException
import com.zorroa.zmlp.service.storage.SystemStorageService
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class AwsSystemStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    @Test
    fun testFetchTypeRefObject() {
        val blob1 = listOf("spock", "bones", "kirk")
        systemStorageService.storeObject("/crew/members.json", blob1)

        val blob2 = systemStorageService.fetchObject("/crew/members.json", Json.LIST_OF_STRING)
        assertEquals(blob1, blob2)
    }

    @Test
    fun testFetchScalarType() {
        val clip1 = Clip("foo", 1.0.bd(), 2.0.bd(), "hats")
        systemStorageService.storeObject("/crew/members.json", clip1)

        val clip2 = systemStorageService.fetchObject("/crew/members.json", Clip::class.java)
        assertEquals(clip1.type, clip2.type)
    }

    @Test(expected = SystemStorageException::class)
    fun testDeleteObject() {
        val blob1 = listOf("spock", "bones", "kirk")
        systemStorageService.storeObject("/crew/members.json", blob1)

        val blob2 = systemStorageService.fetchObject("/crew/members.json", Json.LIST_OF_STRING)
        assertEquals(blob1, blob2)

        systemStorageService.deleteObject("/crew/members.json")
        systemStorageService.fetchObject("/crew/members.json", Json.LIST_OF_STRING)
    }
}
