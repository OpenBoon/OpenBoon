package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Blob;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BlobServiceTests extends AbstractTest {

    @Autowired
    BlobService blobService;

    @Test
    public void testSet() {
        Blob blob1 = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bar"));
        Blob blob2 = blobService.get("app", "feature", "name");
        assertEquals(blob1, blob2);
    }

    @Test
    public void testSetAndReset() {
        Blob blob1 = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bar"));
        Blob blob2 = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bing"));

        assertEquals(blob1, blob2);
        assertEquals("bing", ((Map) blob2.getData()).get("foo"));
    }
}
