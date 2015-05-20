package com.zorroa.archivist.service;

import java.io.File;

import com.zorroa.archivist.domain.Proxy;

public interface ProxyService {

    /**
     * Create a proxy with the appropriate scale and return a Proxy object.
     *
     * @param original
     * @param scale
     * @return
     */
    Proxy makeProxy(File original, double scale);

    /**
     * Create a proxy with the appropriate size and return a Proxy object.
     *
     * @param original
     * @param scale
     * @return
     */
    Proxy makeProxy(File original, int width, int height);
}
