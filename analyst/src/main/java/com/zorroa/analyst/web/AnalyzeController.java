package com.zorroa.analyst.web;

import com.zorroa.analyst.service.AnalyzeService;
import com.zorroa.sdk.domain.AnalyzeRequest;
import com.zorroa.sdk.domain.AnalyzeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

/**
 * Created by chambers on 2/16/16.
 */
@RestController
public class AnalyzeController {

    @Autowired
    AnalyzeService analyzeService;

    @RequestMapping(value="/api/v1/analyze", method=RequestMethod.POST)
    public AnalyzeResult analyze(@RequestBody AnalyzeRequest request) throws Throwable {
        try {
            return analyzeService.asyncAnalyze(request);
        } catch (ExecutionException e) {
            /**
             * We want the correct exception to be thrown back to the client, not the
             * execution exception which is wrapping the original exception.
             */
            Throwable cause  = e.getCause();
            throw cause;
        }
    }
}
