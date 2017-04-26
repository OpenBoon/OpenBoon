package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.Access;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.archivist.service.FilterService;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 4/26/17.
 */
public class FilterControllerTests extends MockMvcTest {

    @Autowired
    FilterService filterService;

    Asset asset;
    Filter filter;
    FilterSpec spec;

    @Before
    public void init() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        asset = assetService.index(source);
        refreshIndex();
    }

    @Test
    public void testCreate() throws Exception {

        spec = new FilterSpec();
        spec.setSearch(new AssetSearch());
        spec.setAcl(new Acl().addEntry(
                userService.getPermission("group::share"), Access.Read));
        spec.setSearch(new AssetSearch(
                new AssetFilter().addToTerms("origin.service", "local")));
        spec.setDescription("A filter");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/filters")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(status().isOk())
                .andReturn();

        filter = Json.Mapper.readValue(result.getResponse().getContentAsString(), Filter.class);
        assertEquals(spec.getDescription(), filter.getDescription());
        assertTrue(filter.getSearch().getFilter().getTerms().containsKey("origin.service"));
        assertTrue(filter.getAcl().hasAccess( userService.getPermission("group::share"), Access.Read));
    }

    @Test
    public void testGetAll() throws Exception {
        testCreate();

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/filters")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Filter> filters = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Filter>>() {});
        Filter filter = filters.get(0);
        assertEquals(spec.getDescription(), filter.getDescription());
        assertTrue(filter.getSearch().getFilter().getTerms().containsKey("origin.service"));
        assertTrue(filter.getAcl().hasAccess( userService.getPermission("group::share"), Access.Read));
    }

    @Test
    public void testGet() throws Exception {
        testCreate();

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/filters/" + filter.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Filter filter = Json.Mapper.readValue(result.getResponse().getContentAsString(), Filter.class);
        assertEquals(spec.getDescription(), filter.getDescription());
        assertTrue(filter.getSearch().getFilter().getTerms().containsKey("origin.service"));
        assertTrue(filter.getAcl().hasAccess( userService.getPermission("group::share"), Access.Read));
    }
}
