package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.LfsRequest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalFileSystemTests extends AbstractTest {

    @Autowired
    LocalFileSystem lfs;

    @Test
    public void testPathSuggest() {
        LfsRequest req = new LfsRequest();
        req.setPath(resources.resolve("images").toString());
        req.setPrefix("set");

        List<String> paths = lfs.suggest(req);
        assertTrue(paths.contains("set01/"));
        assertFalse(paths.contains("NOTICE"));
    }

    @Test
    public void testPathSuggestDirsHaveSlashes() {
        LfsRequest req = new LfsRequest();
        req.setPath(resources.toString());
        req.setPrefix("im");

        List<String> paths = lfs.suggest(req);
        assertTrue(paths.contains("images/"));
    }

    @Test
    public void testExist() {
        LfsRequest req = new LfsRequest();
        req.setPath(resources.resolve("images").toString());
        assertTrue(lfs.exists(req));

        req.setPath("/etc");
        assertFalse(lfs.exists(req));
    }

    @Test
    public void testPathSuggestFiltered() {
        LfsRequest req = new LfsRequest();
        req.setPath("/show");

        Map<String, List<String>> paths = lfs.listFiles(req);
        assertTrue(paths.get("files").isEmpty());
        assertTrue(paths.get("dirs").isEmpty());
    }

    @Test
    public void testPathSuggestByType() {
        LfsRequest req = new LfsRequest();
        req.setPath(resources.resolve("images/set06").toString());
        req.setTypes(ImmutableSet.of("cr2"));

        List<String> suggested = lfs.suggest(req);
        assertEquals(1, suggested.size());

    }
}
