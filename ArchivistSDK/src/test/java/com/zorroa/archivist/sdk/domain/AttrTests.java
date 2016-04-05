package com.zorroa.archivist.sdk.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 3/21/16.
 */
public class AttrTests {

    @Test
    public void testAttr() {
        assertEquals("a.b.c.d", Attr.attr("a","b","c","d"));
    }

    @Test
    public void testName() {
        assertEquals("d", Attr.name("a.b.c.d"));
    }

    @Test
    public void testNamespace() {
        assertEquals("a.b.c", Attr.namespace("a.b.c.d"));
    }
}
