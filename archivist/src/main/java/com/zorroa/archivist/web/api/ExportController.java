package com.zorroa.archivist.web.api;

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.archivist.service.ExportService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
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
    EventLogService logService;

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
    public ResponseEntity<FileSystemResource> getExport(@PathVariable int id) {
        Job job = jobService.get(id);
        /**
         * Don't let people download other people's exports, as its not possible
         * to know if they have access to each individual file.
         */
        if (job.getUser().getId() != SecurityUtils.getUser().getId()) {
            throw new ArchivistReadException("Invalid export for " +  SecurityUtils.getUsername());
        }
        if (!job.getState().equals(JobState.Finished)) {
            throw new ArchivistReadException("Export is not complete.");
        }

        logExportDownload(id);

        File file = new File((String)job.getArgs().get("outputFile"));

        HttpHeaders headers = new HttpHeaders();
        headers.add("content-disposition", "attachment; filename=" + job.getName() + ".zip");
        headers.setContentType(MediaType.valueOf("application/zip"));
        headers.setContentLength(file.length());
        ResponseEntity response = new ResponseEntity(new FileSystemResource(file), headers, HttpStatus.OK);
        return response;
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
        logService.logAsync(UserLogSpec.build(LogAction.Export, "asset", ids.toArray()));
    }
}
