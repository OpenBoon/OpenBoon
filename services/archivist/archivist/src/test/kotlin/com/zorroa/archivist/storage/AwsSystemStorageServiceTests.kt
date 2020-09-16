package com.zorroa.archivist.storage

import com.zorroa.archivist.AbstractTest
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
        val data = mapOf("foo" to "bar")
        systemStorageService.storeObject("/crew/members.json", data)

        val data2 = systemStorageService.fetchObject("/crew/members.json", Map::class.java)
        assertEquals("bar", data2["foo"])
    }
}
