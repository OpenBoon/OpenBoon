package com.zorroa.archivist.rest

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.service.SharedLinkService
import com.zorroa.common.util.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

/**
 * Created by chambers on 7/11/17.
 */
class SharedLinkControllerTests : MockMvcTest() {

    @Autowired
    internal var sharedLinkService: SharedLinkService? = null

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val session = admin()

        val spec = SharedLinkSpec()
        spec.sendEmail = false
        spec.state = mapOf("foo" to "bar")
        spec.userIds = setOf(getUserId())

        val result = mvc.perform(post("/api/v1/shared_link")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val t = deserialize(result, SharedLink::class.java)
        assertEquals(spec.state, t.state)
    }

    @Test
    @Throws(Exception::class)
    fun testGet() {
        val session = admin()

        val spec = SharedLinkSpec()
        spec.sendEmail = false
        spec.state = mapOf<String, Any>("foo" to "bar")
        spec.userIds = setOf(getUserId())
        val link = sharedLinkService!!.create(spec)

        val result = mvc.perform(get("/api/v1/shared_link/" + link.id)
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val t = deserialize(result, SharedLink::class.java)
        assertEquals(spec.state, t.state)
    }
}
