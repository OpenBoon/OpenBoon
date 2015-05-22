package com.zorroa.archivist.service;

import java.util.List;

public interface ClusterService {

    List<String> getActiveNodes();

    boolean isMaster();

}
