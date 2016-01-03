package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.service.ImageService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

public class ImageServiceTests extends ArchivistApplicationTests {

    @Autowired
    ImageService imageService;

    @Test
    public void testGenerateProxyPath() throws IOException {
        String id = "ABCD1234-ABCD1234-ABCD1234-ABCD1234";
        File path = imageService.allocateProxyPath(id, "png");
        logger.info(path.getAbsolutePath());
    }
}
