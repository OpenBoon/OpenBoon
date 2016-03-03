package com.zorroa.analyst.service;

import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;

import java.util.concurrent.ExecutionException;

/**
 * Created by chambers on 2/8/16.
 */
public interface AnalyzeService {

    AnalyzeResult asyncAnalyze(AnalyzeRequest req) throws ExecutionException;

    AnalyzeResult analyze(AnalyzeRequest req);

}
