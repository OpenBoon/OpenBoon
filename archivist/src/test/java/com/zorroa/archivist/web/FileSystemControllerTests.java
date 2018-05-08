package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.zorroa.archivist.domain.OnlineFileCheckRequest;
import com.zorroa.archivist.service.LocalFileSystem;
import com.zorroa.archivist.web.api.FileSystemController;
import com.zorroa.sdk.filesystem.ObjectFile;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileSystemControllerTests extends MockMvcTest {
    final static Logger logger = LoggerFactory.getLogger(FileSystemController.class);

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    FileSystemController fileSystemController;

    @Autowired
    ObjectFileSystem ofs;

    @Autowired
    LocalFileSystem lfs;

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

    @Test
    public void testSuggest() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/lfs/_suggest")
                .session(session)
                .content(Json.serializeToString(
                        ImmutableMap.of("path", FileUtils.normalize(resources.resolve("images/set01").toString()))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<String> paths = deserialize(result, List.class);
        assertTrue(paths.contains("faces.jpg"));
        assertTrue(paths.contains("hyena.jpg"));
        assertTrue(paths.contains("toucan.jpg"));
        assertTrue(paths.contains("visa.jpg"));
        assertTrue(paths.contains("visa12.jpg"));
    }

    @Test
    public void testCheckOnline() throws Exception {
        MockHttpSession session = admin();

        addTestAssets("set04");
        refreshIndex();

        OnlineFileCheckRequest req = new OnlineFileCheckRequest(new AssetSearch());
        MvcResult result = mvc.perform(post("/api/v1/lfs/_online")
                .session(session)
                .content(Json.serializeToString(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> rsp = deserialize(result, Map.class);
        assertEquals(6, rsp.get("total"));
        assertEquals(6, rsp.get("totalOnline"));
        assertEquals(0, rsp.get("totalOffline"));
    }
}
