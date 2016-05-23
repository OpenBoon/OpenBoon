package com.zorroa.archivist.web;

import com.zorroa.archivist.service.AnalystService;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 3/3/16.
 */
public class AnalystControllerTests extends MockMvcTest {

    @Autowired
    AnalystService analystService;

    AnalystPing ping;

    @Before
    public void init() {
        ping = getAnalystPing();
        analystService.register(ping);
    }

    @Test
    public void testRegister() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/analyst/_register")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(ping)))
                .andExpect(status().isOk())
                .andReturn();

        List<Analyst> all = analystService.getAll();
        assertEquals("https://192.168.100.100:8080", all.get(0).getUrl());
    }
}
