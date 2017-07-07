package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/1/16.
 */
public class PluginControllerTests  extends MockMvcTest {

    @Test
    public void testPluginUpload() throws Exception {

        MockMultipartFile plugin =
                new MockMultipartFile("file", "zorroa-test-plugin.zip", "application/zip",
                        new FileInputStream(resources.resolve("plugins/zorroa-test-plugin.zip").toFile()));

        MockHttpSession session = admin();

        MvcResult result = mvc.perform(MockMvcRequestBuilders.fileUpload("/api/v1/plugins")
                .file(plugin)
                .session(session))
                .andExpect(status().is(200))
                .andReturn();

        Plugin p = Json.deserialize(result.getResponse().getContentAsByteArray(), Plugin.class);
        assertEquals("zorroa-test", p.getName());
    }


}
