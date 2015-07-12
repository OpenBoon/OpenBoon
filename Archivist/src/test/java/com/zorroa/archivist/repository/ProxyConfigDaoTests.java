package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyConfigBuilder;
import com.zorroa.archivist.domain.ProxyConfigUpdateBuilder;
import com.zorroa.archivist.domain.ProxyOutput;
import org.elasticsearch.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProxyConfigDaoTests extends ArchivistApplicationTests {

    @Autowired
    ProxyConfigDao proxyConfigDao;

    ProxyConfig proxyConfig;

    @Before
    public void init() {
        ProxyConfigBuilder builder = new ProxyConfigBuilder();
        builder.setName("test");
        builder.setDescription("test proxy config.");
        builder.setOutputs(Lists.newArrayList(
                new ProxyOutput("png", 128, 8),
                new ProxyOutput("png", 256, 8),
                new ProxyOutput("png", 1024, 8)
        ));
        proxyConfig  = proxyConfigDao.create(builder);
    }

    @Test
    public void testGetAll() {
        List<ProxyConfig> configs = proxyConfigDao.getAll();
        assertEquals(2, configs.size());
    }

    @Test
    public void getById() {
        ProxyConfig config2 = proxyConfigDao.get(proxyConfig.getId());
        assertEquals(config2.getId(), proxyConfig.getId());
        assertEquals(config2.getName(), proxyConfig.getName());
    }

    @Test
    public void getByName() {
        ProxyConfig config2 = proxyConfigDao.get(proxyConfig.getName());
        assertEquals(config2.getName(), proxyConfig.getName());
    }

    @Test
    public void update() {
        ProxyConfigUpdateBuilder builder = new ProxyConfigUpdateBuilder();
        builder.setDescription("this is not a test");
        builder.setName("not_test");
        builder.setOutputs(Lists.newArrayList(
                new ProxyOutput("png", 128, 8)
        ));
        proxyConfigDao.update(proxyConfig, builder);
    }

}
