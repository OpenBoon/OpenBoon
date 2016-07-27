package com.zorroa.archivist.web;

import com.zorroa.archivist.AbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

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
        String uri = "a/b/c/1/2/3/bogus.jpg";
         mvc.perform(get("/api/v1/fs/proxies/" + uri))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }
}
