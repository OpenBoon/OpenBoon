package com.zorroa.archivist.service;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;

public class ClusterServiceTests extends ArchivistApplicationTests {

    @Autowired
    ClusterService clusterService;

    @Test
    public void getActiveNodes() {
        List<String> nodes = clusterService.getActiveNodes();
        logger.info("{}", nodes);
    }

    @Test
    public void isMaster() {
        /*
         * There is only 1 node in a unit test so this is
         * always true.
         */
        assertTrue(clusterService.isMaster());
    }
}
