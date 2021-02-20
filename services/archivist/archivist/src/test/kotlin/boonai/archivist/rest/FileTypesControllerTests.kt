package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

class FileTypesControllerTests : MockMvcTest() {

    @Test
    fun testGetImageTypes() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/file-types/images")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$[0]", CoreMatchers.equalTo("bmp")))
            .andReturn()
    }

    @Test
    fun testGetVideoTypes() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/file-types/videos")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$[0]", CoreMatchers.equalTo("avi")))
            .andReturn()
    }

    @Test
    fun testGetDocumentTypes() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/file-types/documents")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$[0]", CoreMatchers.equalTo("doc")))
            .andReturn()
    }

    @Test
    fun testGetMediaType() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/media-type/jpg")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.media-type", CoreMatchers.equalTo("image/jpeg")))
            .andReturn()
    }
}
