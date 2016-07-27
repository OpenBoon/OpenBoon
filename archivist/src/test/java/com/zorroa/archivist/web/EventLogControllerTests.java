package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.common.domain.*;
import com.zorroa.common.repository.EventLogDao;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 12/29/15.
 */
public class EventLogControllerTests extends MockMvcTest {

    @Autowired
    EventLogDao eventLogDao;

    @Before
    public void init() {
        eventLogDao.setSynchronous(true);
        for (int i=0; i<10; i++) {
            eventLogDao.info(EventSpec.log("log message #{}", i).setTags(Sets.newHashSet("bilbo" + i)));
        }
        refreshIndex("eventlog", 10);
    }

    @Test
    public void testGetAllEmptySearch() throws Exception {
        long current = eventLogDao.getAll(new EventSearch(), Paging.first()).getPage().getTotalCount();
        MvcResult result = mvc.perform(post("/api/v1/eventlog/_search")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        logger.info("{}", result.getResponse().getContentAsString());

        PagedList<Event> hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<PagedList<Event>>() {});
        assertEquals(10, (long)hits.getPage().getTotalCount());
    }

    @Test
    public void testGetAllBySearch() throws Exception {

        EventSearch search = new EventSearch();
        search.setTags(Sets.newHashSet("bilbo1"));

        MvcResult result = mvc.perform(post("/api/v1/eventlog/_search")
                .session(admin())
                .content(Json.serialize(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        PagedList<Event> hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<PagedList<Event>>() {});
        assertEquals(1, hits.size());
    }

    @Test
    public void testCountEmptySearch() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/eventlog/_count")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        Map map = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        assertEquals(10, map.get("count"));
    }

    @Test
    public void testCountBySearch() throws Exception {

        EventSearch search = new EventSearch();
        search.setTags(Sets.newHashSet("bilbo1"));

        MvcResult result = mvc.perform(post("/api/v1/eventlog/_count")
                .session(admin())
                .content(Json.serialize(search))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map hits = Json.Mapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        assertEquals(1, hits.get("count"));
    }
}
