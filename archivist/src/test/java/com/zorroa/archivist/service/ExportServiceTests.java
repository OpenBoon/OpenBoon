package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.common.domain.ExecuteTaskRequest;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 8/16/16.
 */
public class ExportServiceTests extends AbstractTest {

    @Autowired
    ExportService exportService;

    @Autowired
    JobExecutorService jobExecutorService;

    Job job;
    ExportSpec spec;
    Asset asset;

    @Before
    public void init() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "cat");
        asset = assetService.index(source);
        refreshIndex();
    }


    @Test
    public void testCreate() {

        spec = new ExportSpec();
        spec.setName("test");
        spec.setSearch(new AssetSearch().setQuery("cats"));
        job = exportService.create(spec);
        int count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, count);
    }

    @Test
    public void testCreateWithFields() throws IOException {
        spec = new ExportSpec();
        spec.setName("test");
        spec.setFields(ImmutableList.of("source.mediaType", "source.filename"));
        spec.setSearch(new AssetSearch().setQuery("cats"));
        job = exportService.create(spec);

        List<ExecuteTaskStart> tasks =
                jobExecutorService.getWaitingTasks(new ExecuteTaskRequest().setCount(5));

        ZpsScript script = Json.Mapper.readValue(
                new File(tasks.get(0).getScriptPath()), ZpsScript.class);
        for (ProcessorRef ref: script.getExecute()) {
            if (ref.getClassName().endsWith("MetadataExporter")) {
                assertEquals((List)spec.getFields(),ref.getArgs().get("fields"));
            }
        }
    }
}
