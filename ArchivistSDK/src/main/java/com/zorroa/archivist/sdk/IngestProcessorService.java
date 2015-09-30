/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk;

import java.io.File;

public interface IngestProcessorService {

    // Works for distribution, test and development resources
    public abstract File getResourceFile(String path);

    // Translates filename and extension into loadable image file
    public abstract File getProxyFile(String filename, String extension);

    // Returns true if the asset is a valid image file
    public abstract boolean isImage(AssetBuilder asset);
}
