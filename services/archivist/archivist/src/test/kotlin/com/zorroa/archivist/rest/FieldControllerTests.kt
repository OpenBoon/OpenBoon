package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.service.IndexRoutingService
import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class FieldControllerTests : MockMvcTest() {

    @Test
    fun testMapping() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/fields/_mapping")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

}