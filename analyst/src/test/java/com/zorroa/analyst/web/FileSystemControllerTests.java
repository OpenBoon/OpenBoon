package com.zorroa.analyst.web;

import com.zorroa.analyst.AbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileSystemControllerTests extends AbstractTest {
    final static Logger logger = LoggerFactory.getLogger(FileSystemController.class);

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    FileSystemController fileSystemController;

    protected MockMvc mvc;

    @Before
    public void setup() throws IOException {
        this.mvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .build();
    }

    @Test
    public void testBrokenProxy() throws Exception {
        try {
            String uri = "a/b/c/1/2/3/bogus.jpg";
            MvcResult result = mvc.perform(get("/api/v1/fs/proxies/" + uri))
                    .andExpect(status().is4xxClientError())
                    .andReturn();
            fail();
        } catch (Exception e) {
//            logger.debug("Successfully caught invalid proxy exception {}", e);
            assertEquals(NestedServletException.class, e.getClass());
            assertEquals("java.lang.IllegalArgumentException: Invalid object ID: bogus.jpg", e.getCause().getMessage());
        }
    }
}
