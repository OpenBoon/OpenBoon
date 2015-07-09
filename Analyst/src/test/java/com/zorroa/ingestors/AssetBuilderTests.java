package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.AssetBuilder;
import com.zorroa.archivist.sdk.IngestProcessor;
import com.zorroa.archivist.sdk.IngestProcessorServiceBaseImpl;
import com.zorroa.archivist.sdk.Proxy;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AssetBuilderTests {

    public static final Logger logger = LoggerFactory.getLogger(AssetBuilderTests.class);

    protected Set<AssetBuilder> testAssets;

    public AssetBuilderTests() {
        logger.info("Setting unit test");
    }

    @Before
    public void setup() {
        IngestProcessor.ingestProcessorService = new IngestProcessorServiceBaseImpl();

        testAssets = new HashSet<AssetBuilder>(2);
        File imageFolder = IngestProcessor.ingestProcessorService.getResourceFile("/images");
        File proxyFolder = IngestProcessor.ingestProcessorService.getResourceFile("/proxies");
        File[] images = imageFolder.listFiles();
        for (File file : images) {
            if (!file.isFile())
                continue;
            String filename = file.getName();
            int extIndex = filename.lastIndexOf('.');
            if (extIndex < 0)    // Check only last name component first
                continue;
            AssetBuilder asset = new AssetBuilder(file.getAbsolutePath());
            List<Proxy> proxyList = new ArrayList<Proxy>();
            File proxyFile = new File(proxyFolder + "/" + filename.substring(0, extIndex) + "-proxy.png");
            if (proxyFile.exists()) {
                Proxy proxy = new Proxy();
                proxy.setFile(proxyFile.getName());
                BufferedImage image = null;
                try {
                    image = ImageIO.read(proxyFile);
                    proxy.setWidth(image.getWidth());
                    proxy.setHeight(image.getHeight());
                    proxy.setFormat("png");
                    proxyList.add(proxy);
                    asset.document.put("proxies", proxyList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            testAssets.add(asset);
        }
    }

    public File getTestImage(String name) {
        return IngestProcessor.ingestProcessorService.getResourceFile("/images" + "/" + name);
    }

}
