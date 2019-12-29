package com.zorroa.auth.server.rest

import com.zorroa.auth.client.Permission
import com.zorroa.auth.server.MockMvcTest
import com.zorroa.auth.server.domain.ApiKeySpec
import java.util.UUID
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class AuthControllerTests : MockMvcTest() {

    @Test
    fun testAuthExtenrnalToken() {
        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/auth-token")
                .headers(superAdmin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.permissions",
                    CoreMatchers.hasItem("ProjectOverride")
                )
            )
            .andReturn()
    }

    @Test
    fun testAuthUserToken() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead),
            UUID.randomUUID()
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
