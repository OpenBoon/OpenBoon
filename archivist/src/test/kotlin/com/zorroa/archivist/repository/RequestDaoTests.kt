package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.domain.RequestType
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class RequestDaoTests : AbstractTest() {

    @Autowired
    private lateinit var requestDao: RequestDao

    @Test
    fun testCreateAndGet() {
        val spec = RequestSpec()
        spec.folderId = folderService.get("/Library")!!.id
        spec.comment = "foo"
        spec.type = RequestType.Export

        val req = requestDao.create(spec)

        assertEquals(spec.type, req.type)
        assertEquals(spec.comment, req.comment)
        assertEquals(spec.folderId, req.folderId)
    }
}
