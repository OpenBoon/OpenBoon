package com.zorroa.archivist.service;

import com.google.common.io.Files;
import com.zorroa.archivist.AbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 6/30/16.
 */
public class PluginServiceTests extends AbstractTest {

    @Autowired
    PluginService pluginService;

    @Before
    public void init() {

    }

    @Test
    public void testInstallPlugin() throws IOException {
        Files.copy(new File("src/test/resources/plugins/zorroa-test-plugin.zip.bak"),
                new File("src/test/resources/plugins/zorroa-test-plugin.zip"));

        assertEquals(0, pluginService.getPlugins().size());
        pluginService.registerPlugin(Paths.get("src/test/resources/plugins/zorroa-test-plugin.zip"));
        refreshIndex();

        assertEquals(1, pluginService.getPlugins().size());
    }
}
