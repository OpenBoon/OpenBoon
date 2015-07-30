package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Snapshot;
import com.zorroa.archivist.domain.SnapshotBuilder;
import com.zorroa.archivist.domain.SnapshotRestoreBuilder;
import com.zorroa.archivist.domain.SnapshotState;
import com.zorroa.archivist.repository.SnapshotDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SnapshotServiceImpl implements SnapshotService {

    @Autowired
    SnapshotDao snapshotDao;

    @Override
    public Snapshot create(SnapshotBuilder builder) {
        return snapshotDao.create(builder);
    }

    @Override
    public boolean delete(Snapshot snapshot) {
        return snapshotDao.delete(snapshot);
    }

    @Override
    public boolean restore(Snapshot snapshot, SnapshotRestoreBuilder builder) {
        return snapshotDao.restore(snapshot, builder);
    }

    @Override
    public Snapshot get(String name) {
        return snapshotDao.get(name);
    }

    @Override
    public List<Snapshot> getAll() {
        return snapshotDao.getAll();
    }

    @Override
    public List<Snapshot> getAll(SnapshotState state) {
        return snapshotDao.getAll(state);
    }

}
