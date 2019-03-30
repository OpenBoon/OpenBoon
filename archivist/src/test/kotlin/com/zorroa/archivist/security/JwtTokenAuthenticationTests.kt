package com.zorroa.archivist.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.rest.MockMvcTest
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class JwtTokenAuthenticationTests : MockMvcTest() {

    @Test
    @Throws(Exception::class)
    fun testWho() {
        val user = userService.get("admin")
        val token = generateUserToken(user.id, userService.getHmacKey(user))

        val rsp = mvc.perform(MockMvcRequestBuilders.get("/api/v1/who")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .header(JwtSecurityConstants.HEADER_STRING,
                        "${JwtSecurityConstants.TOKEN_PREFIX}$token")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val who = Json.Mapper.readValue<Map<String, Any>>(rsp.response.contentAsString)
        assertEquals("admin", who["username"])
    }
}
