package com.zorroa.archivist.service;

import java.io.File;
import java.io.IOException;

import com.zorroa.archivist.domain.Proxy;
import com.zorroa.archivist.domain.ProxyOutput;

public interface ProxyService {

    /**
     * Make a proxy image for the supplied original file using the given proxy output.
     *
     * @param original
     * @param output
     * @return
     */
    Proxy makeProxy(File original, ProxyOutput output) throws IOException;
}
