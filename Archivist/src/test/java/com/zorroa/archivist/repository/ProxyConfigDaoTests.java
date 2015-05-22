package com.zorroa.archivist.repository;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.ProxyConfig;

public class ProxyConfigDaoTests extends ArchivistApplicationTests {

    @Autowired
    ProxyConfigDao proxyConfigDao;

    @Value("${archivist.index.alias}")
    private String alias;

    @Test
    public void testGetAll() throws InterruptedException {
        List<ProxyConfig> configs = proxyConfigDao.getAll();
        assertEquals(1, configs.size());
    }
}
