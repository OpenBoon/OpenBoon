package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.service.AnalystService;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.util.Json;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 12/22/15.
 */
public class ConfigControllerTests extends MockMvcTest {

    @Autowired
    AnalystService analystService;

    /**
     * Not sure this is even necessary.
     * @throws Exception
     */
    @Ignore
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

    @Test
    public void testGetProcessors() throws Exception {
        AnalystPing ping = getAnalystPing();
        analystService.register(ping);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/plugins/processors")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<ProcessorProperties> data = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<ProcessorProperties>>() {});
        assertEquals(4, data.size());
    }

    @Test
    public void testGetProcessorsByType() throws Exception {
        AnalystPing ping = getAnalystPing();
        analystService.register(ping);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/plugins/processors/0")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<ProcessorProperties> data = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<ProcessorProperties>>() {});
        assertEquals(1, data.size());
    }

    @Test
    public void testGetProcessorsByTypeName() throws Exception {
        AnalystPing ping = getAnalystPing();
        analystService.register(ping);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/plugins/processors/ingest")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<ProcessorProperties> data = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<ProcessorProperties>>() {});
        assertEquals(1, data.size());
    }
}
