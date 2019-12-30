package com.zorroa.auth.server.rest

import com.zorroa.auth.server.MockMvcTest
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class PermissionControllerTests : MockMvcTest() {

    @Test
    fun testGetAll() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/permissions")
                .headers(superAdmin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$[0].name",
                    CoreMatchers.equalTo("SystemMonitor")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$[0].description",
                    CoreMatchers.equalTo("Allows access to monitoring endpoints")
                )
            )
            .andReturn()
    }
}
