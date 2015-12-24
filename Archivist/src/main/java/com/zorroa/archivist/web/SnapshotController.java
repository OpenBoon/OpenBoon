package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Snapshot;
import com.zorroa.archivist.domain.SnapshotBuilder;
import com.zorroa.archivist.domain.SnapshotRestoreBuilder;
import com.zorroa.archivist.service.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
@RestController
public class SnapshotController {

    @Autowired
    SnapshotService snapshotService;


    @RequestMapping(value = "/api/v1/snapshots", method = RequestMethod.POST)
    public Snapshot create(@RequestBody SnapshotBuilder builder) {
        return snapshotService.create(builder);
    }

    @RequestMapping(value = "/api/v1/snapshots", method = RequestMethod.GET)
    public List<Snapshot> getAll() {
        return snapshotService.getAll();
    }

    @RequestMapping(value = "/api/v1/snapshots/{name}", method = RequestMethod.GET)
    public Snapshot get(@PathVariable String name) {
        return snapshotService.get(name);
    }

    @RequestMapping(value = "/api/v1/snapshots/{name}/_restore", method = RequestMethod.PUT)
    public boolean restore(@PathVariable String name, @RequestBody SnapshotRestoreBuilder builder) {
        Snapshot snapshot = snapshotService.get(name);
        return snapshotService.restore(snapshot, builder);
    }

    @RequestMapping(value = "/api/v1/snapshots/{name}", method = RequestMethod.DELETE)
    public boolean delete(@PathVariable String name) {
        Snapshot snapshot = snapshotService.get(name);
        return snapshotService.delete(snapshot);
    }

}
