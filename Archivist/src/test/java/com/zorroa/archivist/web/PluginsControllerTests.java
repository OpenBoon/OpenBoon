package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 1/6/16.
 */
public class PluginsControllerTests extends MockMvcTest {

    @Test
    public void testGetAll() throws Exception {
        MockHttpSession session = admin();

        MvcResult result = mvc.perform(get("/api/v1/plugins/ingestors")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<String> ingestors = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<String>>() {});
        assertTrue(ingestors.contains("com.zorroa.archivist.ingestors.ImageIngestor"));
        assertTrue(ingestors.contains("com.zorroa.archivist.ingestors.VideoIngestor"));
        assertTrue(ingestors.contains("com.zorroa.archivist.ingestors.PdfIngestor"));

        for (String ingestor: ingestors) {
            assertTrue(ingestor.startsWith("com.zorroa.archivist.ingestors"));
        }

    }

}
