package com.zorroa.analyst;

import com.zorroa.analyst.service.AnalyzeService;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 2/17/16.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@TestPropertySource("/test.properties")
@WebAppConfiguration
public abstract class AbstractTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ApplicationProperties applicationProperties;

    @Autowired
    protected ObjectFileSystem objectFileSystem;

    @Autowired
    protected Client client;

    public AbstractTest() {
        System.setProperty("zorroa.unittest", "true");
    }

    @Before
    public void __init() {
        client.admin().indices().prepareDelete("_all").get();
    }

    public IngestProcessor initIngestProcessor(IngestProcessor p) {
        p.setObjectFileSystem(objectFileSystem);
        p.setApplicationProperties(applicationProperties);
        p.init();
        return p;
    }

    protected File getResourceFile(String path) {
        URL resourceUrl = getClass().getResource(path);
        try {
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return resourcePath.toFile();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Set<AssetBuilder> ingestTestAssets(List<IngestProcessor> pipeline) {
        Set<AssetBuilder> testAssets = new HashSet<AssetBuilder>(2);
        File imageFolder = getResourceFile("/images");
        File proxyFolder = getResourceFile("/proxies");
        File[] images = imageFolder.listFiles();
        for (File file : images) {
            if (!file.isFile())
                continue;
            String filename = file.getName();
            int extIndex = filename.lastIndexOf('.');
            if (extIndex < 0)    // Check only last name component first
                continue;
            AssetBuilder asset = ingestFile(file, pipeline);
            testAssets.add(asset);
        }
        for (IngestProcessor processor: pipeline) {
            processor.teardown();
        }
        return testAssets;
    }

    protected AssetBuilder ingestFile(File file, List<IngestProcessor> pipeline) {
        AssetBuilder asset = new AssetBuilder(file.getAbsolutePath());
        try {
            asset.getSource().setType(AnalyzeService.Tika.detect(asset.getFile()));
        } catch (IOException e) {
            logger.warn("Failed to detect type: {}", e);
        }

        for (IngestProcessor processor: pipeline) {
            processor.process(asset);
        }
        return asset;
    }
}
