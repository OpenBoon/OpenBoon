package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 4/18/17.
 */
public class DyhierarchyControllerTests  extends MockMvcTest {


    @Before
    public void init() throws ParseException {
        for (File f: getTestImagePath("set01").toFile().listFiles()) {
            if (!f.isFile() || f.isHidden()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("tree.path", ImmutableList.of("/foo/bar/", "/bing/bang/", "/foo/shoe/"));
            assetService.index(ab);
        }
        refreshIndex();
    }

    @Test
    public void testCreate() throws Exception {
        MockHttpSession session = admin();

        Folder folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(folder.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));
        
        MvcResult result = mvc.perform(post("/api/v1/dyhi")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(status().isOk())
                .andReturn();

        DyHierarchy dh = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<DyHierarchy>() {});
        assertEquals(4, dh.getLevels().size());
    }
}
