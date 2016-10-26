package com.zorroa.archivist.web.api;

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.ExportService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.archivist.service.LogService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.exception.ZorroaReadException;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

/**
 * Created by chambers on 7/11/16.
 */
@RestController
public class ExportController {

    @Autowired
    ExportService exportService;

    @Autowired
    JobService jobService;

    @Autowired
    SearchService searchService;

    @Autowired
    LogService logService;

    @Autowired
    ApplicationProperties properties;

    @RequestMapping(value="/api/v1/exports", method= RequestMethod.POST)
    public Object create(@RequestBody ExportSpec spec) {
        return exportService.create(spec);
    }

    @RequestMapping(value="/api/v1/exports/{id}", method= RequestMethod.GET)
    public Object get(@PathVariable int id) {
        // TODO check username or admin
        return jobService.get(id);
    }

    /**
     * Stream an export.
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/v1/exports/{id}/_stream", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<FileSystemResource> getExport(@PathVariable int id) {
        Job job = jobService.get(id);
        /**
         * Don't let people download other people's exports, as its not possible
         * to know if they have access to each individual file.
         */
        if (!job.getUserCreated().equals(SecurityUtils.getUsername())) {
            throw new ZorroaReadException("Invalid export " + job.getUserCreated() + " / " + SecurityUtils.getUsername());
        }
        if (!job.getState().equals(JobState.Finished)) {
            throw new ZorroaReadException("Export is not complete.");
        }

        logExportDownload(id);

        File file = new File((String)job.getArgs().get("outputFile"));
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/zip"))
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    private void logExportDownload(int id) {
        List<String> ids = Lists.newArrayList();
        AssetSearch search = new AssetSearch()
                .setFields(new String[] {})
                .setFilter(new AssetFilter()
                .addToTerms("link.export.id", String.valueOf(id)));

        for (Asset asset : searchService.scanAndScroll(search, 10000)) {
            ids.add(asset.getId());
        }
        logService.log(LogSpec.build(LogAction.Export, "asset", ids.toArray()));
    }
}
