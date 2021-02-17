package boonai.authserver.rest

import boonai.authserver.MockMvcTest
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class PermissionControllerTests : MockMvcTest() {

    /**
     * Only returns non-internal permissions.
     */
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
                    CoreMatchers.equalTo("AssetsRead")
                )
            )
            .andReturn()
    }
}
