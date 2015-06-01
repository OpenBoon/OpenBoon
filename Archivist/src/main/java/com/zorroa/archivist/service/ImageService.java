package com.zorroa.archivist.service;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyOutput;

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
     * Return the list of all proxy configurations.
     *
     * @return List<ProxyConfig>
     */
    List<ProxyConfig> getProxyConfigs();

    /**
     * Get a given proxy configuration.
     *
     * @param id
     * @return
     */
    ProxyConfig getProxyConfig(String id);
}
