package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 6/30/16.
 */
public class PluginServiceTests extends AbstractTest {

    @Autowired
    PluginService pluginService;

    @Test
    public void testInstallPlugin() throws IOException {
        assertEquals(0, pluginService.getAllPlugins().size());
        pluginService.installPlugin(Paths.get("../unittest/resources/plugins/zorroa-test-plugin.zip"));
        assertEquals(1, pluginService.getAllPlugins().size());
    }
}
