package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.UploadImportSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 7/11/16.
 */
public interface ImportService {

    PagedList<Job> getAll(Pager page);

    Job create(UploadImportSpec spec, MultipartFile[] files);

    /**
     * Create a import job with the given import spec.
     *
     * @param spec
     * @return
     */
    Job create(ImportSpec spec);

    Map<String, List<String>> suggestImportPath(String path);
}
