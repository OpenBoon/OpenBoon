package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
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

        MvcResult result = mvc.perform(get("/api/v1/plugins/ingest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<String> ingestors = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<String>>() {});
        assertEquals(6, ingestors.size());
        assertTrue(ingestors.contains("com.zorroa.archivist.TestIngestor"));
        assertTrue(ingestors.contains("com.zorroa.archivist.aggregators.AggregatorIngestor"));
        assertTrue(ingestors.contains("com.zorroa.archivist.aggregators.DateAggregator"));
        assertTrue(ingestors.contains("com.zorroa.archivist.aggregators.IngestPathAggregator"));
        assertTrue(ingestors.contains("com.zorroa.archivist.aggregators.RatingAggregator"));
        assertTrue(ingestors.contains("com.zorroa.archivist.ingestors.PermissionIngestor"));
    }
}
