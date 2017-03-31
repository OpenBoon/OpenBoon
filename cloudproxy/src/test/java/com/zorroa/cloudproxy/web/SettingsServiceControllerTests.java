package com.zorroa.cloudproxy.web;

import com.google.common.collect.ImmutableList;
import com.zorroa.cloudproxy.AbstractTest;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.cloudproxy.service.SettingsService;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 3/30/17.
 */
public class SettingsServiceControllerTests extends AbstractTest {

    @Autowired
    SettingsService settingsService;

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mvc;

    @Before
    public void setup() throws IOException {
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .build();
    }

    @Test
    public void testGet() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Settings settings = Json.deserialize(
                result.getResponse().getContentAsByteArray(), Settings.class);

        assertEquals("https://nohost.com", settings.getArchivistUrl());
        assertEquals("100-100-100-100", settings.getHmacKey());
        assertEquals(ImmutableList.of("/foo"), settings.getPaths());
        assertEquals("0 12 12 12 12 ?", settings.getSchedule());
        assertEquals(4, settings.getThreads());
        assertEquals(22, (int) settings.getPipelineId());
    }

    @Test
    public void testUpdate() throws Exception {

        Settings settings = settingsService.getSettings();
        settings.setHmacKey("foo");

        MvcResult result = mvc.perform(put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings)))
                .andExpect(status().isOk())
                .andReturn();

        settings = Json.deserialize(
                result.getResponse().getContentAsByteArray(), Settings.class);

        assertEquals("https://nohost.com", settings.getArchivistUrl());
        assertEquals("foo", settings.getHmacKey());
        assertEquals(ImmutableList.of("/foo"), settings.getPaths());
        assertEquals("0 12 12 12 12 ?", settings.getSchedule());
        assertEquals(4, settings.getThreads());
        assertEquals(22, (int) settings.getPipelineId());
    }
}
