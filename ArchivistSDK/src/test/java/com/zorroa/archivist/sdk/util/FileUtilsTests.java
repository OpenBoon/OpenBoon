package com.zorroa.archivist.sdk.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 12/9/15.
 */
public class FileUtilsTests {

    String path = "/foo/bar/bing/bang.bong";

    @Test
    public void testBasename() {
        assertEquals("bang", FileUtils.basename(path));
    }

    @Test
    public void testFilename() {
        assertEquals("bang.bong", FileUtils.filename(path));
    }

    @Test
    public void testDirname() {
        assertEquals("/foo/bar/bing", FileUtils.dirname(path));
    }
}
