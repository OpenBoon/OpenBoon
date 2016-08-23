package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.ExportService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.exception.ZorroaReadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * Created by chambers on 7/11/16.
 */
@RestController
public class ExportController {

    @Autowired
    ExportService exportService;

    @Autowired
    JobService jobService;

    @RequestMapping(value="/api/v1/exports", method= RequestMethod.POST)
    public Object create(@RequestBody ExportSpec spec) {
        return exportService.create(spec);
    }

    @RequestMapping(value="/api/v1/exports/{id}", method= RequestMethod.GET)
    public Object get(@PathVariable int id) {
        return exportService.get(id);
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

        File file = new File((String)job.getArgs().get("outputFile"));
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/zip"))
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }


}
