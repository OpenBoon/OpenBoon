package com.zorroa.archivist.sdk.schema;

import com.zorroa.archivist.sdk.domain.Proxy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by chambers on 2/26/16.
 */
public class ProxySchemaTests {

    @Test
    public void testAtLeastThisSize() {
        ProxySchema p = new ProxySchema();
        p.add(new Proxy().setHeight(100).setWidth(100));
        p.add(new Proxy().setHeight(50).setWidth(60));
        p.add(new Proxy().setHeight(500).setWidth(400));
        p.add(new Proxy().setHeight(10).setWidth(20));

        assertNull(p.atLeastThisSize(1000));
        assertEquals(100, p.atLeastThisSize(100).getWidth());
        assertEquals(60, p.atLeastThisSize(51).getWidth());
        assertEquals(20, p.atLeastThisSize(5).getWidth());
    }

    @Test
    public void testThisSizeOrBelow() {
        ProxySchema p = new ProxySchema();
        p.add(new Proxy().setHeight(100).setWidth(100));
        p.add(new Proxy().setHeight(50).setWidth(60));
        p.add(new Proxy().setHeight(500).setWidth(400));
        p.add(new Proxy().setHeight(10).setWidth(20));

        assertEquals(400, p.thisSizeOrBelow(1000).getWidth());
        assertEquals(400, p.thisSizeOrBelow(400).getWidth());
        assertNull(p.thisSizeOrBelow(9));
    }

    @Test
    public void testGetSmallest() {
        ProxySchema p = new ProxySchema();
        p.add(new Proxy().setHeight(100).setWidth(100));
        p.add(new Proxy().setHeight(50).setWidth(60));
        p.add(new Proxy().setHeight(500).setWidth(400));
        p.add(new Proxy().setHeight(10).setWidth(20));

        assertEquals(20, p.getSmallest().getWidth());
    }

    @Test
    public void testGetLargest() {
        ProxySchema p = new ProxySchema();
        p.add(new Proxy().setHeight(100).setWidth(100));
        p.add(new Proxy().setHeight(50).setWidth(60));
        p.add(new Proxy().setHeight(500).setWidth(400));
        p.add(new Proxy().setHeight(10).setWidth(20));

        assertEquals(400, p.getLargest().getWidth());
    }
}
