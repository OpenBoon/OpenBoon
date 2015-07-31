package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Snapshot;
import com.zorroa.archivist.domain.SnapshotBuilder;
import com.zorroa.archivist.domain.SnapshotRestoreBuilder;
import com.zorroa.archivist.domain.SnapshotState;

import java.util.List;

public interface SnapshotDao {

    Snapshot create(SnapshotBuilder builder);

    Snapshot get(String name);

    List<Snapshot> getAll();

    List<Snapshot> getAll(SnapshotState state);

    boolean restore(Snapshot snapshot, SnapshotRestoreBuilder builder);

    boolean delete(Snapshot snapshot);
}
