package com.zorroa.analyst.service;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.sdk.processor.ingest.IngestProcessor;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 4/28/16.
 */
public class PluginServiceTests extends AbstractTest {

    @Autowired
    PluginService pluginService;

    @After
    public void cleanup() throws FileNotFoundException {
        deleteRecursive(new File("src/test/plugins/zorroa-test"));
    }

    @Test
    public void testLoadPlugins() throws Exception {
        pluginService.loadPlugins();
        assertEquals(1, pluginService.getLoadedPlugins().size());
        IngestProcessor p = pluginService.getIngestProcessor("com.zorroa.test.TestIngestor");
        assertEquals("com.zorroa.test.TestIngestor", p.getClass().getName());
    }


}
