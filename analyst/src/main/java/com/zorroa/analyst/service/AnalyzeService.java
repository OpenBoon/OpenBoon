package com.zorroa.analyst.service;

import com.zorroa.sdk.domain.AnalyzeRequest;
import com.zorroa.sdk.domain.AnalyzeResult;

/**
 * Created by chambers on 2/8/16.
 */
public interface AnalyzeService {

    /**
     * Add AnalyzeRequest to queue, return immediately and do not
     * wait for a result.
     *
     * @param req
     */
    void asyncAnalyze(AnalyzeRequest req);

    AnalyzeResult analyze(AnalyzeRequest req);

    AnalyzeResult inlineAnalyze(AnalyzeRequest req);

}
