package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.domain.ProxyOutput;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
     * Make a proxy image for the image file using the given proxy output.
     *
     * @param original
     * @param output
     * @return
     * @throws IOException
     */
    Proxy makeProxy(InputStream original, ProxyOutput output) throws IOException;

    /**
     * Make a proxy image for the image file using the given proxy output.
     *
     * @param original
     * @param output
     * @return
     * @throws IOException
     */
    Proxy makeProxy(BufferedImage original, ProxyOutput output) throws IOException;

    /**
     * Return the default proxy image format.
     *
     * @return
     */
    String getDefaultProxyFormat();

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
