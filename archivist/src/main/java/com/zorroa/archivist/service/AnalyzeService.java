package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.AnalyzeSpec;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Created by chambers on 5/15/17.
 */
public interface AnalyzeService {

    Object analyze(AnalyzeSpec spec, MultipartFile[] file) throws IOException;
}
