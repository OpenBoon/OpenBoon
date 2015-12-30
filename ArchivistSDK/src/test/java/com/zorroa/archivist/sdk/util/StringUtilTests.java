package com.zorroa.archivist.sdk.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 12/30/15.
 */
public class StringUtilTests {

    @Test
    public void testDurationToSeconds() {
        assertEquals(86400 + (2*3600), StringUtil.durationToSeconds("1d2h"));
        assertEquals(86400 * 7, StringUtil.durationToSeconds("1w"));
        assertEquals(10, StringUtil.durationToSeconds("10s"));
        assertEquals(70, StringUtil.durationToSeconds("10s1m"));
    }

    @Test
    public void testDurationToMillis() {
        assertEquals((86400 + (2*3600)) * 1000, StringUtil.durationToMillis("1d2h"));
        assertEquals((86400 * 7) * 1000, StringUtil.durationToMillis("1w"));
        assertEquals(10 * 1000, StringUtil.durationToMillis("10s"));
        assertEquals(70 * 1000, StringUtil.durationToMillis("10s1m"));
    }
}
