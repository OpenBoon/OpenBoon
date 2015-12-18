package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportBuilder;
import com.zorroa.archivist.sdk.domain.ExportFilter;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by chambers on 11/13/15.
 */
@RestController
public class ExportController {

    @Autowired
    ExportService exportService;

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports", method= RequestMethod.POST)
    public Export create(@RequestBody ExportBuilder builder) {
        return exportService.create(builder);
    }

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports/{id}", method=RequestMethod.GET)
    public Export get(@PathVariable int id) {
        return exportService.get(id);
    }


    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports/_search", method=RequestMethod.POST)
    public List<Export> getAll(@RequestBody ExportFilter filter) {
        return exportService.getAll(filter);
    }

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports/{id}/outputs", method=RequestMethod.GET)
    public List<ExportOutput> getAllOutputs(@PathVariable int id) {
        Export export = exportService.get(id);
        return exportService.getAllOutputs(export);
    }

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports/{id}/_duplicate", method=RequestMethod.PUT)
    public Export duplicate(@PathVariable int id) {
        exportService.duplicate(exportService.get(id));
        return exportService.get(id);
    }

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports/{id}/_restart", method=RequestMethod.PUT)
    public Export restart(@PathVariable int id) {
        exportService.restart(exportService.get(id));
        return exportService.get(id);
    }

    @PreAuthorize("hasAuthority('export')")
    @RequestMapping(value="/api/v1/exports/{id}/_cancel", method=RequestMethod.PUT)
    public Export cancel(@PathVariable int id) {
        exportService.cancel(exportService.get(id));
        return exportService.get(id);
    }
}
