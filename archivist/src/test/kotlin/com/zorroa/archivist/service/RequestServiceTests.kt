package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.domain.RequestState
import com.zorroa.archivist.domain.RequestType
import org.junit.Test
import kotlin.test.assertEquals

class RequestServiceTests : AbstractTest() {

    @Test
    fun testCreate() {
        val spec = RequestSpec()
        spec.folderId = folderService.get("/Library")!!.id
        spec.comment = "foo"
        spec.type = RequestType.Export

        val req = requestService.create(spec)
        assertEquals(spec.folderId, req.folderId)
        assertEquals(spec.comment, req.comment)
        assertEquals(spec.type, req.type)
        assertEquals(RequestState.Submitted, req.state)
    }
}
