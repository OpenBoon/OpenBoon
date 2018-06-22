package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;
import com.zorroa.archivist.service.SettingsService;
import com.zorroa.common.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/7/17.
 */
public class SettingsControllerTests extends MockMvcTest {

    @Test
    public void testGetAllNoFilter() throws Exception {
        MockHttpSession session = admin();

        MvcResult result = mvc.perform(get("/api/v1/settings")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Setting> t = deserialize(result, SettingsService.Companion.getListOfSettingsType());
        assertFalse(t.isEmpty());
    }

    @Test
    public void testGetAllFilter() throws Exception {
        MockHttpSession session = admin();
        SettingsFilter filter = new SettingsFilter();
        filter.setCount(5);
        MvcResult result = mvc.perform(get("/api/v1/settings")
                .session(session)
                .content(Json.INSTANCE.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Setting> t = deserialize(result, SettingsService.Companion.getListOfSettingsType());
        assertEquals(5, t.size());
    }

    @Test
    public void testSet() throws Exception {
        MockHttpSession session = admin();

        Map<String,String> settings = ImmutableMap.of(
                "curator.thumbnails.drag-template", "bob");

        MvcResult result = mvc.perform(put("/api/v1/settings/")
                .session(session)
                .content(Json.INSTANCE.serialize(settings))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();


        Map<String, Object> map = deserialize(result, Json.INSTANCE.getGENERIC_MAP());
        assertTrue((boolean) map.get("success"));
    }
}
