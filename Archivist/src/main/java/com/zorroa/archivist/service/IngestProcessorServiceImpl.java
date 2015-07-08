package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.IngestProcessorServiceBaseImpl;
import com.zorroa.archivist.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class IngestProcessorServiceImpl extends IngestProcessorServiceBaseImpl {

    @Autowired
    protected ImageService imageService;

    @Override
    public File getProxyFile(String filename, String extension) {
        return imageService.generateProxyPath(filename, extension);
    }
}
