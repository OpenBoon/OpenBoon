package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.ExportService;
import com.zorroa.archivist.sdk.service.IngestService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 12/2/15.
 */
public class ExportServiceTests extends ArchivistApplicationTests {

    @Autowired
    ExportExecutorService exportExecutorService;

    @Autowired
    ExportService exportService;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    SearchService searchService;

    Export export;

    @Before
    public void init() {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex(1000);

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch();
        search.setQuery("beer");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");
        outputFactory.setArgs(ImmutableMap.of("zipEntryPath", ""));

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportService.create(builder);
    }

    @Test
    public void testRestart() {
        SecurityContextHolder.getContext().setAuthentication(null);
        exportExecutorService.execute(export);
        authenticate();

        exportService.restart(export);

        Export export2 = exportService.get(export.getId());
        assertEquals(ExportState.Queued, export2.getState());

        SecurityContextHolder.getContext().setAuthentication(null);
        exportExecutorService.execute(export);
        authenticate();

        // The second export should have v2 in the path.
        assertTrue(exportService.getAllOutputs(export).get(0).getPath().contains("/v2/"));
    }
}
