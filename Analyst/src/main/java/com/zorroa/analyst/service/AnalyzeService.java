package com.zorroa.analyst.service;

import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;

/**
 * Created by chambers on 2/8/16.
 */
public interface AnalyzeService {

    AnalyzeResult analyze(AnalyzeRequest req);
}
