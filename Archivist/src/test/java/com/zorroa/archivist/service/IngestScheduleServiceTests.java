package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 9/18/15.
 */
public class IngestScheduleServiceTests extends ArchivistApplicationTests {


    @Autowired
    IngestScheduleService ingestScheduleService;

    @Autowired
    IngestService ingestService;

    @Test
    public void executeReady() throws InterruptedException {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));

        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setAllDays();
        builder.setRunAtTime(LocalTime.now().plusSeconds(1).toString());
        builder.setName("test");
        builder.setIngestIds(Lists.newArrayList(ingest.getId()));
        ingestScheduleService.create(builder);

        // Wait till schedule is ready to run.
        Thread.sleep(1000);
        assertEquals(1, ingestScheduleService.executeReady());

        // Now, it shouldn't execute again because the next time to execute is tomorrow.
        assertEquals(0, ingestScheduleService.executeReady());
    }
}
