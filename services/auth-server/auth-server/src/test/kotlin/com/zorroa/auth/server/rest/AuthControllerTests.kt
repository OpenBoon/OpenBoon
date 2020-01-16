package com.zorroa.auth.server.rest

import com.zorroa.auth.server.MockMvcTest
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.zmlp.apikey.Permission
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class AuthControllerTests : MockMvcTest() {

    @Test
    fun testAuthInceptionToken() {
        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/auth-token")
                .headers(superAdmin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.permissions",
                    CoreMatchers.hasItem("SystemProjectOverride")
                )
            )
            .andReturn()
    }

    @Test
    fun testAuthUserToken() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
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
