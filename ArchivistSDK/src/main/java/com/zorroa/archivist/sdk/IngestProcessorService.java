/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by wex on 7/4/15.
 */
public interface IngestProcessorService {

    // Works for distribution, test and development resources
    public abstract File getResourceFile(String path);

    // Translates filename and extension into loadable image file
    public abstract File getProxyFile(String filename, String extension);

    // Returns true if the asset is a valid image file
    public abstract boolean isImage(AssetBuilder asset);
}
