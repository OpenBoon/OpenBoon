package com.zorroa.analyst;

import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Created by chambers on 2/17/16.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@TestPropertySource("/test.properties")
@WebAppConfiguration
public abstract class AbstractTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ApplicationProperties applicationProperties;

    @Autowired
    protected ObjectFileSystem objectFileSystem;

    public IngestProcessor initIngestProcessor(IngestProcessor p) {
        p.setObjectFileSystem(objectFileSystem);
        p.setApplicationProperties(applicationProperties);
        p.init();
        return p;
    }

}
