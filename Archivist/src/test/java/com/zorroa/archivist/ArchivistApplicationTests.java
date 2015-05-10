package com.zorroa.archivist;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ArchivistApplication.class)
@WebAppConfiguration
public abstract class ArchivistApplicationTests {

    public static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    public String getStaticImagePath() {
        FileSystemResource resource = new FileSystemResource("src/test/resources/static/images");
        String path = resource.getFile().getAbsolutePath();
        logger.info("test image path: {}", path);
        return path;
    }
}
