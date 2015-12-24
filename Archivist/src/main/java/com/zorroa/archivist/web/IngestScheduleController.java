package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import com.zorroa.archivist.service.IngestScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by chambers on 9/18/15.
 */
@PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
@RestController
public class IngestScheduleController {

    @Autowired
    IngestScheduleService ingestScheduleService;

    @RequestMapping(value="/api/v1/ingestSchedules", method= RequestMethod.POST)
    public IngestSchedule create(@RequestBody IngestScheduleBuilder builder) {
        return ingestScheduleService.create(builder);
    }

    @RequestMapping(value="/api/v1/ingestSchedules/{id}", method= RequestMethod.GET)
    public IngestSchedule get(@PathVariable int id) {
        return ingestScheduleService.get(id);
    }

    @RequestMapping(value="/api/v1/ingestSchedules", method= RequestMethod.GET)
    public List<IngestSchedule> getAll() {
        return ingestScheduleService.getAll();
    }

    @RequestMapping(value="/api/v1/ingestSchedules/{id}", method= RequestMethod.PUT)
    public void update(@RequestBody IngestSchedule updated, @PathVariable Integer id) {
        // just verify it exists
        ingestScheduleService.get(id);
        ingestScheduleService.update(updated);
    }
}
