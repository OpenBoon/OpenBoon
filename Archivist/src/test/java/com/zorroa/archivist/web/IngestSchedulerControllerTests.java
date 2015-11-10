package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.IngestBuilder;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.IngestScheduleService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 9/18/15.
 */
public class IngestSchedulerControllerTests extends MockMvcTest {

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestScheduleService ingestScheduleService;

    public Ingest createIngest() {
        IngestBuilder ib = new IngestBuilder();
        ib.setPath(getStaticImagePath());
        ib.setFileTypes(Sets.newHashSet("jpg"));
        return ingestService.createIngest(ib);
    }

    @Test
    public void testCreate() throws Exception {

        Ingest ingest = createIngest();

        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setIngestIds(Lists.newArrayList(ingest.getId()));
        builder.setRunAtTime("10:00:00");
        builder.setDays(Lists.newArrayList(DayOfWeek.FRIDAY));
        builder.setName("Friday Morning");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/ingestSchedules")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        IngestSchedule schedule = Json.Mapper.readValue(response, IngestSchedule.class);
        assertEquals(builder.getIngestIds(), schedule.getIngestIds());
        assertEquals(builder.getName(), schedule.getName());
        assertEquals(builder.getDays(), schedule.getDays());
        assertEquals(builder.getRunAtTime(), schedule.getRunAtTime());
    }

    @Test
    public void testGet() throws Exception {

        Ingest ingest = createIngest();

        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setIngestIds(Lists.newArrayList(ingest.getId()));
        builder.setRunAtTime("10:00:00");
        builder.setDays(Lists.newArrayList(DayOfWeek.FRIDAY));
        builder.setName("Friday Morning");
        IngestSchedule schedule1 = ingestScheduleService.create(builder);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/ingestSchedules/" + schedule1.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        IngestSchedule schedule2 = Json.Mapper.readValue(response, IngestSchedule.class);

        assertEquals(schedule1.getId(), schedule2.getId());
        assertEquals(schedule1.getIngestIds(), schedule2.getIngestIds());
        assertEquals(schedule1.getName(), schedule2.getName());
        assertEquals(schedule1.getDays(), schedule2.getDays());
        assertEquals(schedule1.getRunAtTime(), schedule2.getRunAtTime());

    }

    @Test
    public void testGetAll() throws Exception {

        Ingest ingest = createIngest();
        for (int i = 0; i < 10; i++) {
            IngestScheduleBuilder builder = new IngestScheduleBuilder();
            builder.setIngestIds(Lists.newArrayList(ingest.getId()));
            builder.setRunAtTime(LocalTime.of(i, 0, 0).toString());
            builder.setAllDays();
            builder.setName("All the times");
            ingestScheduleService.create(builder);
        }

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/ingestSchedules")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<IngestSchedule> schedules = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<IngestSchedule>>() {});
        assertEquals(10, schedules.size());
    }

    @Test
    public void testUpdate() throws Exception {

        Ingest ingest = createIngest();

        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setIngestIds(Lists.newArrayList(ingest.getId()));
        builder.setRunAtTime("10:00:00");
        builder.setDays(Lists.newArrayList(DayOfWeek.FRIDAY));
        builder.setName("Friday Morning");

        IngestSchedule schedule1 = ingestScheduleService.create(builder);
        schedule1.setRunAtTime("11:11:11");
        schedule1.setDays(Lists.newArrayList(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        schedule1.setName("MWF");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/ingestSchedules/" + schedule1.getId())
                .content(Json.serialize(schedule1))
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        IngestSchedule schedule2 = ingestScheduleService.get(schedule1.getId());
        assertEquals(schedule1.getId(), schedule2.getId());
        assertEquals(schedule1.getIngestIds(), schedule2.getIngestIds());
        assertEquals(schedule1.getName(), schedule2.getName());
        assertEquals(schedule1.getDays(), schedule2.getDays());
        assertEquals(schedule1.getRunAtTime(), schedule2.getRunAtTime());
    }
}