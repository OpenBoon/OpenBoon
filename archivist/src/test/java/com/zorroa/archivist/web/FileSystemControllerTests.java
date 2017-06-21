package com.zorroa.archivist.web;

import com.google.common.io.Files;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileSystemControllerTests extends MockMvcTest {
    final static Logger logger = LoggerFactory.getLogger(FileSystemController.class);

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    FileSystemController fileSystemController;

    @Autowired
    ObjectFileSystem ofs;

    @Test
    public void testBrokenProxy() throws Exception {
        String uri = "a/b/c/1/2/3/bogus.jpg";
         mvc.perform(get("/api/v1/ofs/proxies/" + uri))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void testGetFile() throws Exception {
        MockHttpSession session = admin();

        ObjectFile f = ofs.prepare("bing", UUID.randomUUID().toString(), "jpg");
        Files.copy(resources.resolve("images/set01/faces.jpg").toFile(), f.getFile());

        String url = "/api/v1/ofs/" + f.getId();
        mvc.perform(get(url)
                .session(session))
                .andExpect(status().is(200))
                .andReturn();
    }
}
