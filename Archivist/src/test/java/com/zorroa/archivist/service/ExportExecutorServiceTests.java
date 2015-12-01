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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 11/16/15.
 */
public class ExportExecutorServiceTests extends ArchivistApplicationTests {

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
    public void testExecuteExport() {

        /**
         * Log out the current user to ensure the test authenticates.
         */
        SecurityContextHolder.getContext().setAuthentication(null);

        exportExecutorService.execute(export);
        refreshIndex(1000);

        /**
         * The export executor service logs the user out after
         * exporting, which will the rest of this test.  So
         * we'll just re-authenticate.
         */
        authenticate();

        List<Integer> exports = new ArrayList<>();
        exports.add(export.getId());

        AssetFilter filter = new AssetFilter().setExportIds(exports);
        AssetSearch search = new AssetSearch().setFilter(filter);
        AssetSearchBuilder builder = new AssetSearchBuilder().setSearch(search);

        // Assert the export id has been added to the asset.
        assertEquals(1, searchService.search(builder).getHits().getHits().length);

        List<ExportOutput> outputs = exportService.getAllOutputs(export);
        assertTrue(new File(outputs.get(0).getPath()).exists());
    }
}
