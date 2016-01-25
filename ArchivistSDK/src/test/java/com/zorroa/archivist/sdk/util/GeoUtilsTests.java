package com.zorroa.archivist.sdk.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 1/25/16.
 */
public class GeoUtilsTests {

    @Test
    public void testNearestPlace() {
        assertEquals("Newman", GeoUtils.nearestPlace(-23.456, 123.456));
    }
}
