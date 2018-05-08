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
        val spec = RequestSpec(folderService.get("/Library")!!.id,
                RequestType.Export,
                "foo")
        val req = requestDao.create(spec)

        assertEquals(spec.type, req.type)
        assertEquals(spec.comment, req.comment)
        assertEquals(spec.folderId, req.folderId)
    }
}
