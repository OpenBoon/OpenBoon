package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.DisplayProperties;
import com.zorroa.sdk.processor.ProcessorType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 5/23/16.
 */
public class PluginDaoTests extends AbstractTest {

    @Autowired
    PluginDao pluginDao;

    PluginProperties plugin;

    int id;

    @Before
    public void init() {
        plugin = new PluginProperties();
        plugin.setDescription("A test plugin");
        plugin.setVersion("1.0");
        plugin.setName("Foo Plugin");
        id = pluginDao.create(plugin);
    }

    @Test
    public void testGetPlugins() {
        assertEquals(1, pluginDao.getPlugins().size());
    }

    @Test
    public void testValidatePluginProperties() {
        PluginProperties plugin2 = pluginDao.getPlugins().get(0);
        assertEquals(plugin.getDescription(), plugin2.getDescription());
        assertEquals(plugin.getVersion(), plugin2.getVersion());
        assertEquals(plugin.getProcessors(), plugin2.getProcessors());
    }

    @Test
    public void testGetProcessors() {
        assertEquals(0, pluginDao.getProcessors().size());
        sendAnalystPing();
        assertEquals(3, pluginDao.getProcessors().size());
    }

    @Test
    public void updateProcessor() {
        sendAnalystPing();
        int pid = this.jdbc.queryForObject("SELECT pk_plugin FROM plugin WHERE str_name=?", Integer.class, "FooBar");
        pluginDao.addProcessor(pid, pluginDao.getProcessors(ProcessorType.Ingest).get(0)
                .setDisplay(Lists.newArrayList(new DisplayProperties().setName("rambo"))));
        assertEquals("rambo", pluginDao.getProcessors(ProcessorType.Ingest).get(0).getDisplay().get(0).getName());
    }

    @Test
    public void testGetProcessorsByType() {
        assertEquals(0, pluginDao.getProcessors(ProcessorType.Ingest).size());
        sendAnalystPing();
        assertEquals(1, pluginDao.getProcessors(ProcessorType.Ingest).size());
    }

    @Test
    public void testCreateDuplicatePlugin() {
        PluginProperties plugin = new PluginProperties();
        plugin.setDescription("A test plugin");
        plugin.setVersion("1.0");
        plugin.setName("Foo Plugin");
        int id1 = pluginDao.create(plugin);
        int id2 = pluginDao.create(plugin);
        assertEquals(id1, id2);
    }
}
