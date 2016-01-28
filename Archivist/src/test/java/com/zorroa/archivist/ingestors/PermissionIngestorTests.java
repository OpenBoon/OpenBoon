package com.zorroa.archivist.ingestors;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.ExportService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.service.ExportExecutorService;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.SearchService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 1/28/16.
 */
public class PermissionIngestorTests extends ArchivistApplicationTests {

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    SearchService searchService;

    @Autowired
    AssetService assetService;

    @Autowired
    ExportExecutorService exportExecutorService;

    @Autowired
    ExportService exportService;

    @Test
    public void testSetSearchPermission() {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("permissions");
        builder.addToProcessors(
                new ProcessorFactory<>(PermissionIngestor.class, ImmutableMap.of("search", Lists.newArrayList("group::superuser"))));

        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(TEST_DATA_PATH).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        assertEquals(0, searchService.search(new AssetSearch().setQuery("beer")).getHits().totalHits());
    }

    @Test(expected=AccessDeniedException.class)
    public void testSetWritePermission() {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("permissions");
        builder.addToProcessors(
                new ProcessorFactory<>(PermissionIngestor.class, ImmutableMap.of("write", Lists.newArrayList("group::superuser"))));

        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(TEST_DATA_PATH).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        Asset asset = assetService.getAll().get(0);
        assetService.update(asset.getId(), new AssetUpdateBuilder().setRating(2));
    }

    @Test
    public void testSetExportPermission() {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("permissions");
        builder.addToProcessors(
                new ProcessorFactory<>(PermissionIngestor.class, ImmutableMap.of("export", Lists.newArrayList("group::superuser"))));

        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("standard")).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);


        AssetSearch search = new AssetSearch();
        search.setQuery("beer");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");
        outputFactory.setArgs(ImmutableMap.of("zipEntryPath", ""));

        ExportBuilder eBuilder = new ExportBuilder();
        eBuilder.setNote("An export for Bob");
        eBuilder.setOptions(options);
        eBuilder.setSearch(search);
        eBuilder.setOutputs(com.google.common.collect.Lists.newArrayList(outputFactory));

        Export export = exportService.create(eBuilder);
        exportExecutorService.execute(export);
        refreshIndex(1000);
        
        authenticate();

        List<Integer> exports = new ArrayList<>();
        exports.add(export.getId());

        AssetFilter filter = new AssetFilter().setExportIds(exports);
        AssetSearch exportSearch = new AssetSearch().setFilter(filter);
        // Assert nothing exported
        assertEquals(0, searchService.search(exportSearch).getHits().getTotalHits());

    }
}
