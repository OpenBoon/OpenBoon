package com.zorroa.archivist.web;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyConfigBuilder;
import com.zorroa.archivist.domain.ProxyConfigUpdateBuilder;
import com.zorroa.archivist.domain.ProxyOutput;
import com.zorroa.archivist.service.ImageService;
import org.elasticsearch.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/12/15.
 */
public class ProxyConfigControllerTests extends MockMvcTest {


    @Autowired
    ImageService imageService;

    ProxyConfig proxyConfig;

    @Before
    public void init() {
        ProxyConfigBuilder builder = new ProxyConfigBuilder();
        builder.setName("test");
        builder.setDescription("test proxy config.");
        builder.setOutputs(Lists.newArrayList(
                new ProxyOutput("png", 128, 8),
                new ProxyOutput("png", 256, 8),
                new ProxyOutput("png", 1024, 8)
        ));
        proxyConfig  = imageService.createProxyConfig(builder);
    }

    @Test
    public void update() throws Exception {

        ProxyConfigUpdateBuilder builder = new ProxyConfigUpdateBuilder();
        builder.setName("foo");
        builder.setDescription("bar");
        builder.setOutputs(Lists.newArrayList(
                new ProxyOutput("bmp", 128, 8)));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/proxy-configs/" + proxyConfig.getId())
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        ProxyConfig updated = Json.Mapper.readValue(result.getResponse().getContentAsString(), ProxyConfig.class);
        assertEquals(proxyConfig.getId(), updated.getId());
        assertEquals(builder.getDescription(), updated.getDescription());
        assertEquals(builder.getName(), updated.getName());
        assertEquals(builder.getOutputs(), updated.getOutputs());
    }
}

