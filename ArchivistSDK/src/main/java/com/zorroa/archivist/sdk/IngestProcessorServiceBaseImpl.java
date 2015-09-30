/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IngestProcessorServiceBaseImpl implements IngestProcessorService {

    @Override
    public File getResourceFile(String path) {
        URL resourceUrl = getClass().getResource(path);
        try {
            Path resourcePath = Paths.get(resourceUrl.toURI());
            return new File(resourcePath.toUri());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public File getProxyFile(String filename, String extension) {
        File proxyFile = getResourceFile("/proxies");
        return new File(proxyFile.getAbsoluteFile() + "/" + filename + "." + extension);
    }

    @Override
    public boolean isImage(AssetBuilder asset) {
        return true;    // FIXME: Works in com.zorroa.archivist.service.IngestProcessorServiceImpl
    }
}
