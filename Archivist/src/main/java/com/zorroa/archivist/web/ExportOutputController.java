package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by chambers on 11/18/15.
 */
@RestController
public class ExportOutputController {

    @Autowired
    ExportService exportService;

    @PreAuthorize("hasAuthority('export')")
    @ResponseBody
    @RequestMapping(value = "/api/v1/outputs/{outputId}/_download", method=RequestMethod.GET)
    public FileSystemResource download(
            @PathVariable("outputId") int outputId,
            HttpServletResponse response) {
        ExportOutput output = exportService.getOutput(outputId);
        response.setContentType(output.getMimeType());
        return new FileSystemResource(output.getPath());
    }

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/outputs/{id}", method=RequestMethod.GET)
    public ExportOutput get(@PathVariable int id) {
        return exportService.getOutput(id);
    }

}
