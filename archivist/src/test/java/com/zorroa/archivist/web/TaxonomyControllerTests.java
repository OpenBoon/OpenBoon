package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
import com.zorroa.archivist.service.TaxonomyService;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 6/22/17.
 */
public class TaxonomyControllerTests extends MockMvcTest {

    @Autowired
    TaxonomyService taxonomyService;

    @Test
    public void testCreate() throws Exception {
        MockHttpSession session = admin();

        Folder f = folderService.create(new FolderSpec().setName("bob").setParentId(0));
        TaxonomySpec spec = new TaxonomySpec(f);

        MvcResult result = mvc.perform(post("/api/v1/taxonomy")
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Taxonomy t = deserialize(result, Taxonomy.class);
        assertEquals(f.getId(), t.getFolderId());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpSession session = admin();

        Folder f = folderService.create(new FolderSpec().setName("bob").setParentId(0));
        TaxonomySpec spec = new TaxonomySpec(f);
        Taxonomy tax = taxonomyService.create(spec);

        MvcResult result = mvc.perform(delete("/api/v1/taxonomy/" + tax.getTaxonomyId())
                .session(session)
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map m = deserialize(result, Map.class);
        assertTrue((boolean) m.get("success"));
    }

}
