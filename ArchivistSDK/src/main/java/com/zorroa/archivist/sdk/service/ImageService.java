package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.ProxyOutput;
import com.zorroa.archivist.sdk.domain.Proxy;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface ImageService {

    /**
     * Make a proxy image for the supplied original file using the given proxy output.
     *
     * @param original
     * @param output
     * @return
     */
    Proxy makeProxy(File original, ProxyOutput output) throws IOException;

    /**
     *
     * @param imgFile
     * @return
     * @throws IOException
     */
    Dimension getImageDimensions(File imgFile) throws IOException;

    /**
     * Return the set of supported formats the service can operate on.
     *
     * @return
     */
    Set<String> getSupportedFormats();

    /**
     * Generates a file path for the given proxy ID.  This does
     * not create the actual directories.
     *
     * @param id
     * @param format
     * @return
     */
    File generateProxyPath(String id, String format);

    /**
     * Convenience method which takes a proxy file name and returns
     * the absolute path of the proxy.
     *
     * @param name
     * @return
     */
    File generateProxyPath(String name);
}
