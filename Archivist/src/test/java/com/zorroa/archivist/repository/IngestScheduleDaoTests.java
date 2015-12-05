package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.IngestBuilder;
import com.zorroa.archivist.sdk.service.IngestService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 9/5/15.
 */
public class IngestScheduleDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestScheduleDao ingestScheduleDao;

    @Autowired
    IngestService ingestService;

    @Test
    public void create() {
        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setDays(Lists.newArrayList(DayOfWeek.FRIDAY));
        builder.setRunAtTime("10:00:00");
        builder.setName("10AM");
        IngestSchedule schedule = ingestScheduleDao.create(builder);

        assertEquals(builder.getDays(), schedule.getDays());
        assertEquals(builder.getRunAtTime(), schedule.getRunAtTime());
        assertEquals(builder.getName(), schedule.getName());
    }

    @Test
    public void update() {

        IngestBuilder ibuilder = new IngestBuilder(getStaticImagePath());
        Ingest ingest = ingestService.createIngest(ibuilder);

        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setDays(Lists.newArrayList(DayOfWeek.FRIDAY));
        builder.setRunAtTime("10:00:00");
        builder.setName("10AM");
        builder.setIngestIds(Lists.newArrayList(ingest.getId()));
        IngestSchedule schedule = ingestScheduleDao.create(builder);

        schedule.setName("foo");
        schedule.setRunAtTime("13:37:00");
        schedule.setDays(Lists.newArrayList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

        assertTrue(ingestScheduleDao.update(schedule));
        IngestSchedule updated = ingestScheduleDao.get(schedule.getId());

        assertEquals(schedule.getName(), updated.getName());
        assertEquals(schedule.getRunAtTime(), updated.getRunAtTime());
        assertEquals(schedule.getDays(), updated.getDays());
        assertEquals(schedule.getId(), updated.getId());
        assertEquals(schedule.getIngestIds(), updated.getIngestIds());
    }

    @Test
    public void getAllReady() throws InterruptedException {

        /**
         * Make an ingest 1 second into the future, then wait 1 second
         * and it should be ready to run.
         */
        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setAllDays();
        builder.setRunAtTime(LocalTime.now().plusSeconds(1).toString());
        builder.setName("test");
        IngestSchedule schedule = ingestScheduleDao.create(builder);

        Thread.sleep(1000);

        List<IngestSchedule> ready = ingestScheduleDao.getAllReady();
        assertEquals(1, ready.size());
    }

    @Test
    public void determineNextRuntime() {
        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setAllDays();
        builder.setRunAtTime(LocalTime.now().minusHours(2).toString());
        builder.setName("test");
        IngestSchedule schedule = ingestScheduleDao.create(builder);

        /*
         * If the time is earlier than today, than we run today.
         */
        Instant instant = Instant.ofEpochMilli(IngestSchedule.determineNextRunTime(schedule));
        LocalDateTime res = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(LocalDateTime.now().getDayOfWeek().plus(1), res.getDayOfWeek());

        /*
         * If its later, then we run tomorrow.
         */
        schedule.setRunAtTime(LocalTime.now().plusHours(2).toString());
        instant = Instant.ofEpochMilli(IngestSchedule.determineNextRunTime(schedule));
        res = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(LocalDateTime.now().plusHours(2).getDayOfWeek(), res.getDayOfWeek());
    }

    @Test
    public void determineNextRuntimeOnlyOneDay() {

        /*
         * Only 1 day is available to choose.
         */
        DayOfWeek day = LocalDateTime.now().minusDays(2).getDayOfWeek();

        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setDays(Lists.newArrayList(day));
        builder.setRunAtTime(LocalTime.now().minusHours(2).toString());
        builder.setName("test");
        IngestSchedule schedule = ingestScheduleDao.create(builder);

        /*
         * If the time is earlier than today, than we run today.
         */
        Instant instant = Instant.ofEpochMilli(IngestSchedule.determineNextRunTime(schedule));
        LocalDateTime res = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(day, res.getDayOfWeek());

        /*
         * If its later, then we run tomorrow.
         */
        schedule.setRunAtTime(LocalTime.now().plusHours(2).toString());
        instant = Instant.ofEpochMilli(IngestSchedule.determineNextRunTime(schedule));
        res = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(day, res.getDayOfWeek());
    }
}
