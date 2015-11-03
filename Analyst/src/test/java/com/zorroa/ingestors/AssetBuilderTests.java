package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class AssetBuilderTests {

    public static final Logger logger = LoggerFactory.getLogger(AssetBuilderTests.class);

    protected Set<AssetBuilder> testAssets;

    public AssetBuilderTests() {
        logger.info("Setting unit test");
    }

    private File getResourceFile(String path) {
        URL resourceUrl = getClass().getResource(path);
        try {
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return new File(resourcePath.toUri());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setup(IngestProcessor processor) {
        if (processor.getArgs() == null) {
            processor.setArgs(new HashMap<String, Object>());
        }
        testAssets = new HashSet<AssetBuilder>(2);
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
            AssetBuilder asset = new AssetBuilder(file.getAbsolutePath());
            List<Proxy> proxyList = new ArrayList<Proxy>();
            File proxyFile = new File(proxyFolder + "/" + filename.substring(0, extIndex) + "-proxy.png");
            if (proxyFile.exists()) {
                Proxy proxy = new Proxy();
                proxy.setPath(proxyFile.getAbsolutePath());
                BufferedImage image = null;
                try {
                    image = ImageIO.read(proxyFile);
                    proxy.setWidth(image.getWidth());
                    proxy.setHeight(image.getHeight());
                    proxy.setFormat("png");
                    proxyList.add(proxy);
                    asset.getDocument().put("proxies", proxyList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            testAssets.add(asset);
        }
    }
}
