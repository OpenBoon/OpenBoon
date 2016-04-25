package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Snapshot;
import com.zorroa.archivist.domain.SnapshotBuilder;
import com.zorroa.archivist.domain.SnapshotRestoreBuilder;
import com.zorroa.archivist.domain.SnapshotState;

import java.util.List;

public interface SnapshotService {

    Snapshot create(SnapshotBuilder builder);

    boolean delete(Snapshot snapshot);

    boolean restore(Snapshot snapshot, SnapshotRestoreBuilder builder);

    Snapshot get(String name);

    List<Snapshot> getAll();

    List<Snapshot> getAll(SnapshotState state);

}
