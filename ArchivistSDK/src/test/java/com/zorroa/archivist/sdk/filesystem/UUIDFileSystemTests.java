package com.zorroa.archivist.sdk.filesystem;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 3/10/16.
 */
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
    public void testGet() {
        URI uri = URI.create("https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.7.3.tar.gz");
        ObjectFile file = fs.get("elasticsearch", uri, "tar.gz");
        assertEquals("/Users/chambers/src/ArchivistSDK/unittest/uuid-fs-tests/elasticsearch/0/2/c/d/0/8/2/f/02cd082f-3f5c-5403-b208-4ec752cb0410.tar.gz",
                file.getFile().getAbsolutePath());
    }

    @Test
    public void testGetByPath() {
        URI uri = URI.create("https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.7.3.tar.gz");
        ObjectFile file1 = fs.get("elasticsearch", uri, "tar.gz");
        ObjectFile file2 = fs.get("elasticsearch/0/2/c/d/0/8/2/f/02cd082f-3f5c-5403-b208-4ec752cb0410.tar.gz");
        assertEquals(file1, file2);
    }
}
