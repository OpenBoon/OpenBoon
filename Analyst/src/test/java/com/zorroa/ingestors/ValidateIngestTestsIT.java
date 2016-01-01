/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class ValidateIngestTestsIT extends IntegrationTest {

    @Test
    public void testCaffeSearch() throws  IOException {
        Integer count = countQuery("scoreboard");
        assertEquals(2, count.intValue());
    }

    @Test
    public void testFaceSearch() throws  IOException {
        Integer count = countQuery("bigface");
        assertEquals(1, count.intValue());
        count = countQuery("face");
        assertEquals(2, count.intValue());

    }

    @Test
    public void testLogoSearch() throws  IOException {
        Integer count = countQuery("bigvisa");
        assertEquals(1, count.intValue());
    }

    @Test
    public void testRetrosheetSearch() throws IOException {
        Integer count = countQuery("Dodgers");
        assertEquals(1, count.intValue());
    }
}
