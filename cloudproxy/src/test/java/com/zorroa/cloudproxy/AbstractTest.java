package com.zorroa.cloudproxy;

import com.zorroa.sdk.client.ArchivistClient;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by chambers on 3/28/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/test.properties")
@WebAppConfiguration
@SpringBootTest()
public abstract class AbstractTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Before
    @After
    public void resetSettings() throws IOException, InterruptedException {
        FileUtils.copyFile(new File("unittest/orig/config.json"),
                new File("unittest/config/config.json"));
        FileUtils.copyFile(new File("unittest/orig/stats.json"),
                new File("unittest/config/stats.json"));
    }

    /**
     * We need archivist up to run these tests.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Before
    public void checkArchivist() throws IOException, InterruptedException {
        System.setProperty("zorroa.hmac.key", "00000000-0000-0000-0000-000000000000");
        System.setProperty("zorroa.user", "admin");

        try {
            ArchivistClient client = new ArchivistClient();
            client.getConnection().get("/health", Map.class);
            logger.info("Archivist is healthy");
        } catch (Exception e) {
            logger.error("ARCHIVIST NEEDS TO BE RUNNING FOR TESTS");
            throw new RuntimeException("ARCHIVIST NEEDS TO BE RUNNING FOR TESTS");
        }
    }
}
