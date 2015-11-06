package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.domain.ProxyOutput;
import com.zorroa.archivist.sdk.service.ImageService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImageServiceTests extends ArchivistApplicationTests {

    @Autowired
    ImageService imageService;

    @Test
    public void testMakeProxy() throws IOException {
        File original = this.getTestImage("beer_kettle_01.jpg");
        ProxyOutput output = new ProxyOutput("png", 128, 8);
        Proxy proxy = imageService.makeProxy(original, output);
        assertEquals(128, proxy.getHeight());
        List<String> e = Splitter.on('.').limit(2).splitToList(proxy.getPath());
        assertTrue(imageService.generateProxyPath(e.get(0), e.get(1)).exists());
    }

    @Test
    public void testGenerateProxyPath() throws IOException {
        String id = "ABCD1234-ABCD1234-ABCD1234-ABCD1234";
        File path = imageService.generateProxyPath(id, "png");
        logger.info(path.getAbsolutePath());
    }
}
