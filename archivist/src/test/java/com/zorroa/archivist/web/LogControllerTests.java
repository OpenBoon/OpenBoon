package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.service.LogService;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 8/30/16.
 */
public class LogControllerTests extends MockMvcTest {

    @Autowired
    LogService logService;

    @Before
    public void init() throws InterruptedException {
        logService.logAsync(new LogSpec().setAction("test").setMessage("A log test"));
        Thread.sleep(1100);
        refreshIndex();
    }

    @Test
    public void testEmptySearch() throws Exception {

        LogSearch search = new LogSearch();

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/logs/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        assertTrue(sr.containsKey("list"));
        assertTrue(sr.containsKey("page"));
        assertTrue((int) ((Map<String,Object>)sr.get("page")).get("totalCount") > 0);
    }

    @Test
    public void testQueryString() throws Exception {

        LogSearch search = new LogSearch();
        search.setQuery(ImmutableMap.of("query_string", ImmutableMap.of("query", "test")));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/logs/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        assertTrue(sr.containsKey("list"));
        assertTrue(sr.containsKey("page"));
        assertEquals(1, (int) ((Map<String,Object>)sr.get("page")).get("totalCount"));
    }

    @Test
    public void testTermQuery() throws Exception {

        LogSearch search = new LogSearch();
        search.setQuery(ImmutableMap.of("term", ImmutableMap.of("message", "test")));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/logs/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        assertTrue(sr.containsKey("list"));
        assertTrue(sr.containsKey("page"));
        assertTrue((int)((Map<String,Object>)sr.get("page")).get("totalCount") > 0);
    }

    @Test
    public void testTermQueryWithAggs() throws Exception {

        LogSearch search = new LogSearch();
        search.setQuery(ImmutableMap.of("term", ImmutableMap.of("message", "test")));
        search.setAggs(ImmutableMap.of("actions", ImmutableMap.of("terms",
                ImmutableMap.of("field", "action"))));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/logs/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        assertTrue(sr.containsKey("list"));
        assertTrue(sr.containsKey("page"));
        assertTrue(sr.containsKey("aggregations"));
        assertEquals(1, (int) ((Map<String,Object>)sr.get("page")).get("totalCount"));
    }

    @Test
    public void testTermQueryFailure() throws Exception {

        LogSearch search = new LogSearch();
        search.setQuery(ImmutableMap.of("term", ImmutableMap.of("message", "bungle")));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/logs/_search")
                .session(session)
                .content(Json.serializeToString(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map sr = deserialize(result, Map.class);
        assertTrue(sr.containsKey("list"));
        assertTrue(sr.containsKey("page"));
        assertEquals(0, (int) ((Map<String, Object>)sr.get("page")).get("totalCount"));
    }
}
