package com.zorroa.archivist.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyOutput;

public class ProxyServiceTests extends ArchivistApplicationTests {

    @Autowired
    ImageService proxyService;

    @Test
    public void testMakeProxy() throws IOException {
        File original = this.getTestImage("beer_kettle_01.jpg");
        ProxyOutput output = new ProxyOutput("png", 128, 8);
        Proxy proxy = proxyService.makeProxy(original, output);
        assertEquals(128, proxy.getHeight());
        assertTrue(new File(proxy.getPath()).exists());
    }
}
