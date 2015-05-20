package com.zorroa.archivist.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Proxy;

public class ProxyServiceTests extends ArchivistApplicationTests {

    @Autowired
    ProxyService proxyService;


    @Test
    public void testMakeProxyScaled() {
        File original = this.getTestImage("beer_kettle_01.jpg");
        Proxy proxy = proxyService.makeProxy(original, 0.25);

        assertEquals(612, proxy.getWidth());
        assertEquals(816, proxy.getHeight());
        assertTrue(new File(proxy.getPath()).canRead());
    }

    @Test
    public void testMakeProxySize() {
        File original = this.getTestImage("beer_kettle_01.jpg");
        Proxy proxy = proxyService.makeProxy(original, 250, 250);

        assertEquals(250, proxy.getWidth());
        assertEquals(250, proxy.getHeight());
        assertTrue(new File(proxy.getPath()).canRead());
        logger.info(proxy.getPath());
    }
}
