package com.zorroa.auth.rest

import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.domain.Permission
import com.zorroa.auth.domain.Role
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

class AuthControllerTests : MockMvcTest() {

    @Test
    fun testAuthExtenrnalToken() {
        mvc.perform(
                MockMvcRequestBuilders.post("/auth/v1/auth-token")
                        .headers(superAdmin())
        )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.permissions",
                        CoreMatchers.hasItem(Role.SUPERADMIN_ROLE)))
                .andReturn()
    }

    @Test
    fun testAuthUserToken() {
        val spec = ApiKeySpec(
                "test",
                UUID.randomUUID(),
                listOf(Permission.READ_ASSETS)
        )
        val apiKey = apiKeyService.create(spec)
        mvc.perform(
                MockMvcRequestBuilders.post("/auth/v1/auth-token")
                        .headers(standardUser(apiKey))
        )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
    }

    @Test
    fun testAuthFailure() {
        mvc.perform(
                MockMvcRequestBuilders.post("/auth/v1/auth-token")
        )
                .andExpect(MockMvcResultMatchers.status().is4xxClientError)
                .andReturn()
    }
}
