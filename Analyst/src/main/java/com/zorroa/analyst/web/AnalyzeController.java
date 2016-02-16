package com.zorroa.analyst.web;

import com.zorroa.analyst.service.AnalyzeService;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 2/16/16.
 */
@RestController
public class AnalyzeController {

    @Autowired
    AnalyzeService analyzeService;

    @RequestMapping(value="/api/v1/analyze", method= RequestMethod.POST)
    public AnalyzeResult analyze(@RequestBody AnalyzeRequest request) {
        return analyzeService.analyze(request);
    }
}
