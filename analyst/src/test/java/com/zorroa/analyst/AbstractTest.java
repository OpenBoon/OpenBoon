package com.zorroa.analyst;

import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.elastic.ElasticClientUtils;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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
    protected Client client;

    public AbstractTest() {
        System.setProperty("zorroa.unittest", "true");
    }

    private String index = "archivist";

    @Before
    public void __init() throws IOException, ClassNotFoundException {
        /**
         * For analyst, this is only done for unit tests.  In production, the
         * archivist handles creating the index and the mapping.
         */
        ElasticClientUtils.deleteAllIndexes(client);
        ElasticClientUtils.createLatestMapping(client, index);
    }

    public void refreshIndex() {
        ElasticClientUtils.refreshIndex(client);
    }

    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) {
            return true;
        }
        boolean ret = true;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }
}
