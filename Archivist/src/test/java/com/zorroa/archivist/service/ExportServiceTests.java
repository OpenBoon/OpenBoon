package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.ArchivistException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by chambers on 12/2/15.
 */
public class ExportServiceTests extends ArchivistApplicationTests {

    Export export;

    @Before
    public void init() {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
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

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportService.create(builder);
    }

    @Test(expected=ArchivistException.class)
    public void testExceedMaxAssetCount() {
        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");
        outputFactory.setArgs(ImmutableMap.of("zipEntryPath", ""));

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setSearch(new AssetSearch());
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportService.create(builder);
    }

    @Test(expected=ArchivistException.class)
    public void testZeroAssetExport() {
        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");
        outputFactory.setArgs(ImmutableMap.of("zipEntryPath", ""));

        ExportBuilder builder = new ExportBuilder();
        builder.setSearch(new AssetSearch("oieowieowieowieowieoewo"));
        builder.setOutputs(Lists.newArrayList(outputFactory));

        export = exportService.create(builder);
    }

    @Test
    public void testExportProperties() {
        assertEquals(1, export.getAssetCount());
        assertEquals(1800475, export.getTotalFileSize());
    }

    @Test
    public void testRestart() {
        logout();
        exportExecutorService.execute(export);
        authenticate();

        exportService.restart(export);

        Export export2 = exportService.get(export.getId());
        assertEquals(ExportState.Queued, export2.getState());

        logout();
        exportExecutorService.execute(export);
        authenticate();
        refreshIndex();
        // The second export should have v2 in the path.
        logger.info("path {}", exportService.getAllOutputs(export).get(0).getPath());
        assertEquals(2, (int) jdbc.queryForObject("SELECT int_execute_count FROM export WHERE pk_export=?",
                Integer.class, export.getId()));
    }

    @Test
    public void testDuplicate() {
        Export export2 = exportService.duplicate(export);
        assertNotEquals(export.getId(), export2.getId());
        assertEquals(Json.serializeToString(export.getOptions()), Json.serializeToString(export2.getOptions()));
        assertEquals(Json.serializeToString(export.getSearch()), Json.serializeToString(export2.getSearch()));

        List<ExportOutput> outputs1 = exportService.getAllOutputs(export);
        List<ExportOutput> outputs2 = exportService.getAllOutputs(export2);

        assertEquals(outputs1.size(), outputs2.size());
        for (int i=0; i< outputs1.size(); i++) {
            ExportOutput output1 = outputs1.get(i);
            ExportOutput output2 = outputs2.get(i);

            assertNotEquals(output1.getId(), output2.getId());
            assertEquals(output1.getUserCreated(), output2.getUserCreated());
            assertEquals(output1.getFactory().getKlass(), output2.getFactory().getKlass());
            assertEquals(output1.getFileExtention(), output2.getFileExtention());
        }
    }

    @Test
    public void testOfflineExport() {
        SecurityContextHolder.getContext().setAuthentication(null);
        exportExecutorService.execute(export);
        refreshIndex();
        authenticate();

        assertEquals(1, exportService.offline(export));
        assertEquals(0, exportService.offline(export));
    }

    @Test
    public void testOfflineExportOutput() {
        SecurityContextHolder.getContext().setAuthentication(null);
        exportExecutorService.execute(export);
        refreshIndex();
        authenticate();

        assertEquals(true, exportService.offline(exportService.getAllOutputs(export).get(0)));
        assertEquals(false, exportService.offline(exportService.getAllOutputs(export).get(0)));
    }

    @Test
    public void testExportsFolder() {
        SearchResponse r = searchService.search(new AssetSearch().setFilter(
                new AssetFilter().addToFolderIds(folderService.get("/Exports").getId())));
        assertEquals(2, r.getHits().getTotalHits());
    }
}
