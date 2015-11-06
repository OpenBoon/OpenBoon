package com.zorroa.archivist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 11/6/15.
 */
public class FileUtilTests {

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
