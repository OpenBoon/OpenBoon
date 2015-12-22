package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 12/22/15.
 */
public class ConfigControllerTests   extends MockMvcTest {

    @Test
    public void testSupportedFormats() throws Exception {

        MvcResult result = mvc.perform(get("/api/v1/config/supported_formats")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, String> json = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, String>>() {});

        assertTrue(json.containsKey("tif"));
        assertTrue(json.containsKey("tiff"));
        assertTrue(json.containsKey("psd"));
        assertTrue(json.containsKey("gif"));
        assertTrue(json.containsKey("png"));
        assertTrue(json.containsKey("jpg"));
    }
}
