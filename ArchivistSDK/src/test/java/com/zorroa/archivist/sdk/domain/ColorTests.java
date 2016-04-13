package com.zorroa.archivist.sdk.domain;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 4/13/16.
 */
public class ColorTests {

    private static final Logger logger = LoggerFactory.getLogger(ColorTests.class);

    @Test
    public void testGetKeywords() {
        Color c = new Color(255, 10, 10);
        assertEquals("red red", c.getKeywords());

        c = new Color(15, 12, 13);
        assertEquals("black", c.getKeywords());

        c = new Color(1, 1, 1);
        assertEquals("black", c.getKeywords());

        c = new Color(93, 138, 168);
        assertEquals("cyan blue", c.getKeywords());

        c = new Color(255, 255, 255);
        assertEquals("white", c.getKeywords());
    }

}
