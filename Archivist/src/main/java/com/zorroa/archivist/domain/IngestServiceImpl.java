package com.zorroa.archivist.domain;

import com.zorroa.archivist.sdk.IngestServiceBaseImpl;
import com.zorroa.archivist.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * Created by wex on 7/4/15.
 */
public class IngestServiceImpl extends IngestServiceBaseImpl {

    @Autowired
    protected ImageService imageService;

    @Override
    public File getProxyFile(String filename, String extension) {
        return imageService.generateProxyPath(filename, extension);
    }
}
