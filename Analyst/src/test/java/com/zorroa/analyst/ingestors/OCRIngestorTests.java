package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by jbuhler on 2/24/16.
 */
public class OCRIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        OCRIngestor.Option ocrOption = new OCRIngestor.Option();
        ocrOption.targetString = Arrays.asList("SWC", "PHOTOGRAPHY");
        ocrOption.targetStringBox = Arrays.asList(1149,128,798,113);
        ocrOption.namespace = "petrol";
        ocrOption.key = "docType";
        ocrOption.value = "Core photography";

        OCRIngestor.Box box = new OCRIngestor.Box();
        box.boxArea = Arrays.asList(961,238,1292,96);
        box.namespace = "petrol";
        box.key = "wellName";
        box.isKeyword = true;
        ocrOption.boxes = ImmutableList.<OCRIngestor.Box>builder().add(box).build();
        OCRIngestor ocrIngestor = new OCRIngestor();
        ocrIngestor.docTypes = ImmutableList.<OCRIngestor.Option>builder().add(ocrOption).build();

        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(ocrIngestor))
                .build();

        File file = getResourceFile("/images/BHP_SWC_PHOTOGRAPHY.TIF");
        AssetBuilder asset = ingestFile(file, pipeline);

        String wellName = asset.getAttr("petrol:wellName");
        assertEquals("THEBE -1CH  ", wellName);

    }
}
