package com.zorroa.archivist.sdk.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 12/9/15.
 */
public class FileUtilsTests {

    String path = "/foo/bar/bing/bang.bong";

    @Test
    public void testIsURI() {
        assertTrue(FileUtils.isURI("http://foo.bar"));
        assertFalse(FileUtils.isURI("/foo.bar"));
    }

    @Test
    public void testSuperSplit() {
        List<String> parts = FileUtils.superSplit(path);
        assertEquals("/foo", parts.get(0));
        assertEquals("/foo/bar", parts.get(1));
        assertEquals("/foo/bar/bing", parts.get(2));
        assertEquals("/foo/bar/bing/bang.bong", parts.get(3));
        assertEquals(4, parts.size());
    }

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
