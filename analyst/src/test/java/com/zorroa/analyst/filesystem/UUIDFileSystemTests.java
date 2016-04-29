package com.zorroa.analyst.filesystem;

import com.zorroa.sdk.filesystem.ObjectFile;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UUIDFileSystemTests {

    final static Logger logger = LoggerFactory.getLogger(UUIDFileSystemTests.class);

    UUIDFileSystem fs;

    @Before
    public void init() {
        Properties props = new Properties();
        props.setProperty("root", "unittest/uuid-fs-tests");
        fs = new UUIDFileSystem(props);
        fs.init();
    }

    @Test
    public void testGetByCategoryAndId() {
        String name1  = String.format("%s_foo_bar.png", UUID.randomUUID());
        String name2  = String.format("%s.png", UUID.randomUUID());
        fs.get("proxies", name1);
        fs.get("proxies", name2);
    }

    @Test
    public void testPrepare() {
        ObjectFile file = fs.prepare("test", "hello", "txt");
        assertFalse(file.exists());
        assertTrue(file.getFile().getParentFile().exists());
    }
}
