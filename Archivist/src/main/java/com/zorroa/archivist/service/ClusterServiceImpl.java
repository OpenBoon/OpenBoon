package com.zorroa.archivist.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistClusterStateException;
import com.zorroa.archivist.ArchivistConfiguration;

@Component
public class ClusterServiceImpl implements ClusterService {

    @Autowired
    Client client;

    @Autowired
    ArchivistConfiguration config;

    @Override
    public List<String> getActiveNodes() {
        try {
            NodeInfo[] nodes = client.admin()
                    .cluster()
                    .nodesInfo(new NodesInfoRequest().all())
                    .get()
                    .getNodes();

            List<String> result = Lists.newArrayListWithCapacity(nodes.length);
            for (NodeInfo node: nodes) {
                result.add(node.getHostname());
            }
            return result;

        } catch (InterruptedException | ExecutionException e) {
            throw new ArchivistClusterStateException("Failed to determine nodes in cluster: " + e, e);
        }
    }

    @Override
    public boolean isMaster() {
        try {
            String name = config.getName();
            NodeInfo[] nodes = client.admin()
                    .cluster()
                    .nodesInfo(new NodesInfoRequest().all())
                    .get()
                    .getNodes();
            for (NodeInfo node: nodes) {
                if (node.getNode().getName().equals(name)) {
                    return node.getNode().isMasterNode();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ArchivistClusterStateException("Failed to determine nodes in cluster: " + e, e);
        }
        return false;
    }
}
