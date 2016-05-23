package com.zorroa.analyst.service;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 4/28/16.
 */
public class RegisterServiceTests extends AbstractTest {

    @Autowired
    PluginService pluginService;

    @Autowired
    RegisterService registerService;

    @Before
    public void init() {

        pluginService.loadPlugins();
    }

    @Test
    public void testGetPing() {
        AnalystPing ping = registerService.getPing();
        assertEquals("https://127.0.0.1:8099", ping.getUrl());
        PluginProperties plugin = ping.getPlugins().get(0);

        assertEquals("zorroa-test", plugin.getName());
        assertEquals("0.19.0", plugin.getVersion());
        assertEquals("com.zorroa.test.TestPluginBundle", plugin.getClassName());

        ProcessorProperties proc = plugin.getProcessors().get(0);
        assertEquals("com.zorroa.test.TestIngestor", proc.getClassName());
        assertEquals(ProcessorType.Ingest, proc.getType());
    }
}
