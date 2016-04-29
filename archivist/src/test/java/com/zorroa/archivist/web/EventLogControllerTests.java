package com.zorroa.archivist.web;

import com.google.common.collect.Sets;
import com.zorroa.archivist.TestCountResult;
import com.zorroa.archivist.TestSearchResult;
import com.zorroa.archivist.repository.EventLogDao;
import com.zorroa.sdk.domain.EventLogMessage;
import com.zorroa.sdk.domain.EventLogSearch;
import com.zorroa.sdk.util.Json;
import com.zorroa.common.service.EventLogService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 12/29/15.
 */
public class EventLogControllerTests extends MockMvcTest {

    @Autowired
    EventLogDao eventLogDao;

    @Autowired
    EventLogService eventLogService;

    @Before
    public void init() {
        eventLogService.setSynchronous(true);
        for (int i=0; i<10; i++) {
            eventLogSerivce.log(new EventLogMessage("log message #{}", i).setTags(Sets.newHashSet("bilbo" + i)));
        }
        refreshIndex("eventlog", 10);
    }

    @Test
    public void testGetAllEmptySearch() throws Exception {
        long current = eventLogDao.getAll(new EventLogSearch()).getHits().totalHits();
        MvcResult result = mvc.perform(post("/api/v1/eventlog/_search")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        TestSearchResult hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), TestSearchResult.class);
        assertEquals(10, hits.getHits().getTotal());
    }

    @Test
    public void testGetAllBySearch() throws Exception {

        EventLogSearch search = new EventLogSearch();
        search.setTags(Sets.newHashSet("bilbo1"));

        MvcResult result = mvc.perform(post("/api/v1/eventlog/_search")
                .session(admin())
                .content(Json.serialize(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        TestSearchResult hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), TestSearchResult.class);
        assertEquals(1, hits.getHits().getTotal());
    }

    @Test
    public void testCountEmptySearch() throws Exception {
        long current = eventLogDao.getAll(new EventLogSearch()).getHits().totalHits();
        MvcResult result = mvc.perform(post("/api/v1/eventlog/_count")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        TestCountResult hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), TestCountResult.class);
        assertEquals(10, hits.getCount());
    }

    @Test
    public void testCountBySearch() throws Exception {

        EventLogSearch search = new EventLogSearch();
        search.setTags(Sets.newHashSet("bilbo1"));

        MvcResult result = mvc.perform(post("/api/v1/eventlog/_count")
                .session(admin())
                .content(Json.serialize(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        TestCountResult hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), TestCountResult.class);
        assertEquals(1, hits.getCount());
    }
}
