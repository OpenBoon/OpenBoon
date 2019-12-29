package com.zorroa.zmlp.sdk;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTests {

    @Test
    public void testUUIValidity() throws Exception {
        assertTrue(Utils.isValidUUI("D29556D6-8CF7-411B-8EB0-60B573098C26"));
        assertFalse(Utils.isValidUUI("dog"));

    }

}
