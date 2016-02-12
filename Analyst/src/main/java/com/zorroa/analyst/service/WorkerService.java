package com.zorroa.analyst.service;

import com.zorroa.archivist.sdk.domain.AnalyzeRequest;

/**
 * Created by chambers on 2/8/16.
 */
public interface WorkerService {

    void analyze(AnalyzeRequest req);
}
