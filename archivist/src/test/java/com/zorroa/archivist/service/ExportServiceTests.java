package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    Document asset;

    @Before
    public void init() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "cats");
        asset = assetService.index(source);
        refreshIndex();
    }

    @Test
    public void testCreate() {
        ExportSpec spec = new ExportSpec();
        spec.setName("test");
        spec.setSearch(new AssetSearch().setQuery("cats"));

        job = exportService.create(spec);
        int count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, count);
    }

    @Test
    public void testCreateAndGetExportFile() {
        ExportSpec spec = new ExportSpec();
        spec.setName("test");
        spec.setSearch(new AssetSearch().setQuery("cats"));
        job = exportService.create(spec);

        ExportFile file1 = exportService.createExportFile(job, new ExportFileSpec()
                .setMimeType("application/octet-stream")
                .setName("foo.zip")
                .setSize(100));
        ExportFile file2 = exportService.getExportFile(file1.getId());
        assertEquals(file1, file2);
        assertEquals(file1.getJobId(), file2.getJobId());
        assertEquals(file1.getMimeType(), file2.getMimeType());
        assertEquals(file1.getName(), file2.getName());
        assertEquals(file1.getSize(), file2.getSize());
    }

    @Test
    public void testGetAllExportFiles() {

        ExportSpec spec = new ExportSpec();
        spec.setName("test");
        spec.setSearch(new AssetSearch().setQuery("cats"));
        job = exportService.create(spec);

        for (int i=0; i<10; i++) {
            exportService.createExportFile(job, new ExportFileSpec()
                    .setMimeType("application/octet-stream")
                    .setName("foo"+ i +".zip")
                    .setSize(1024));
        }

        List<ExportFile> files = exportService.getAllExportFiles(job);
        assertEquals(10, files.size());
    }
}
