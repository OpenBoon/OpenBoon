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
    public abstract ClassLoader getSiteClassLoader();

    public abstract File getResourceFile(String path);

    public abstract File getProxyFile(String filename, String extension);
}
