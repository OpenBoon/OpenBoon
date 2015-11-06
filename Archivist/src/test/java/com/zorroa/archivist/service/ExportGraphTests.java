package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.processors.export.AssetInput;
import com.zorroa.archivist.processors.export.CompressedFileOutput;
import com.zorroa.archivist.processors.export.FolderOutput;
import com.zorroa.archivist.processors.export.ReformatImage;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.io.IOException;

/**
 * Created by chambers on 11/4/15.
 */
public class ExportGraphTests extends ArchivistApplicationTests {

    @Autowired
    ExportExecutorService exportExecutorService;

    @Autowired
    AssetDao assetDao;

    @Before
    public void init() throws IOException {

        String path = getStaticImagePath() + "/beer_kettle_01.jpg";

        AssetBuilder builder = new AssetBuilder(path);
        builder.setSearchPermissions(userService.getPermissions().get(0));
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);
    }

    @Test
    public void testExecuteSimpleExportPipeline() {
        AssetInput inputAsset = new AssetInput();
        FolderOutput folderOutput = new FolderOutput();
        inputAsset.outputPath.cord().connect(folderOutput.inputPath.socket());

        ExportGraph graph = exportExecutorService.getExportGraph();
        graph.execute(folderOutput);
    }

    @Test
    public void testExecuteComplexExportPipeline() {

        CompressedFileOutput zipFile = new CompressedFileOutput();
        zipFile.zipEntryPath.setValue("%{source.basename}");

        AssetInput inputAsset = new AssetInput();

        ReformatImage reformat1 = new ReformatImage();
        reformat1.size.setValue(new Dimension(100, 100));
        reformat1.basename.setValue("%{source.basename}_100sq");

        ReformatImage reformat2 = new ReformatImage();
        reformat2.size.setValue(new Dimension(800, 600));
        reformat2.basename.setValue("%{source.basename}_800x600");

        reformat1.outputPath.cord().connect(zipFile.inputPath.socket());
        reformat2.outputPath.cord().connect(zipFile.inputPath.socket());

        inputAsset.outputPath.cord().connect(reformat1.inputPath.socket());
        inputAsset.outputPath.cord().connect(reformat2.inputPath.socket());

        ExportGraph graph = exportExecutorService.getExportGraph();
        graph.execute(zipFile);
    }
}
