package com.zorroa.archivist.web;

import com.google.common.collect.Lists;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.util.Json;
import com.zorroa.archivist.service.AnalystService;
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

    @Test
    public void testRegister() throws Exception {

        AnalystPing ping = new AnalystPing();
        ping.setUrl("https://192.168.100.100:8080");
        ping.setData(false);
        ping.setThreadsTotal(1);
        ping.setProcessFailed(0);
        ping.setProcessSuccess(1);
        ping.setQueueSize(1);
        ping.setThreadsActive(0);
        ping.setIngestProcessorClasses(Lists.newArrayList());

        MvcResult result = mvc.perform(post("/api/v1/analyst/_register")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(ping)))
                .andExpect(status().isOk())
                .andReturn();

        List<Analyst> all = analystService.getAll();
        assertEquals("https://127.0.0.1:8080", all.get(0).getUrl());
    }

}
