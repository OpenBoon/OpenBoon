package com.zorroa.analyst.service;

import com.zorroa.sdk.filesystem.ObjectFile;

import java.io.IOException;
import java.net.URI;

/**
 * Created by chambers on 3/9/16.
 */
public interface TransferService {

    void transfer(URI src, ObjectFile dst) throws IOException;

    void transfer(String src, ObjectFile dst) throws IOException;
}
