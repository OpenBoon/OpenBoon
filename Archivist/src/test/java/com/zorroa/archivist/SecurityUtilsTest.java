package com.zorroa.archivist;

import org.junit.Test;

public class SecurityUtilsTest {

    @Test
    public void testcreatePasswordHash() {

        System.out.println(SecurityUtils.createPasswordHash("foo"));

    }
}
