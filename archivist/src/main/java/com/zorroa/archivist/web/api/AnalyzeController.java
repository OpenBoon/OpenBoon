package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.AnalyzeSpec;
import com.zorroa.archivist.service.AnalyzeService;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Created by chambers on 5/15/17.
 */
@RestController
public class AnalyzeController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeController.class);

    @Autowired
    AnalyzeService analyzeService;

    /**
     *
     * @param body
     * @param files
     * @return
     * @throws IOException
     */
    @PostMapping(value="/api/v1/analyze/_files", consumes = {"multipart/form-data"})
    @ResponseBody
    public Object analyzeUpload(@RequestParam("files") MultipartFile[] files,
                       @RequestParam("body") String body) throws IOException {
        AnalyzeSpec spec = Json.deserialize(body, AnalyzeSpec.class);
        return analyzeService.analyze(spec, files);
    }

    /**
     *
     * @param spec
     * @return
     * @throws IOException
     */
    @PostMapping(value="/api/v1/analyze/_assets")
    @ResponseBody
    public Object analyzeAssets(@RequestBody AnalyzeSpec spec) throws IOException {
        return analyzeService.analyze(spec, null);
    }
}
