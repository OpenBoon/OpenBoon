package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Snapshot;
import com.zorroa.archivist.domain.SnapshotBuilder;
import com.zorroa.archivist.domain.SnapshotRestoreBuilder;
import com.zorroa.archivist.domain.SnapshotState;
import com.zorroa.common.elastic.AbstractElasticDao;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SnapshotDaoImpl extends AbstractElasticDao implements SnapshotDao {

    @Value("${archivist.snapshot.repoName}")
    private String snapshotRepoName;

    @Override
    public String getType() {
        return "snapshot";
    }

    private Boolean snapshotExists(String name) {
        GetSnapshotsRequestBuilder builder =
                new GetSnapshotsRequestBuilder(client.admin().cluster());
        builder.setRepository(snapshotRepoName);
        GetSnapshotsResponse getSnapshotsResponse = builder.execute().actionGet();
        List<SnapshotInfo> snapshots = getSnapshotsResponse.getSnapshots();
        for (SnapshotInfo snapshot : snapshots) {
            if (snapshot.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Snapshot snapshotFromInfo(SnapshotInfo info) {
        // FIXME: Should this use info.toXContent()?
        Snapshot snapshot = new Snapshot();
        snapshot.setName(info.name());
        snapshot.setState(getState(info.state()));
        snapshot.setStartTime(info.startTime());
        snapshot.setEndTime(info.endTime());
        return snapshot;
    }

    @Override
    public Snapshot create(SnapshotBuilder builder) {
        String name = builder.getName();
        if (snapshotExists(name)) {
            return null;
        }

        CreateSnapshotRequestBuilder requestBuilder = new CreateSnapshotRequestBuilder(client.admin().cluster());
        requestBuilder.setRepository(snapshotRepoName)
                .setSnapshot(name);
        requestBuilder.execute().actionGet();
        Snapshot snapshot = get(name);
        return snapshot;
    }

    // FIXME: We could assign the enum constants to match and cast?
    private SnapshotState getState(org.elasticsearch.snapshots.SnapshotState state) {
        switch (state) {
            case IN_PROGRESS: return SnapshotState.InProgress;
            case SUCCESS: return SnapshotState.Success;
            case FAILED: return SnapshotState.Failed;
            case PARTIAL: return SnapshotState.Partial;
        }
        return SnapshotState.Failed;
    }

    private List<SnapshotInfo> getAllSnapshotInfos() {
        GetSnapshotsRequestBuilder builder =
                new GetSnapshotsRequestBuilder(client.admin().cluster());
        builder.setRepository(snapshotRepoName);
        GetSnapshotsResponse getSnapshotsResponse = builder.execute().actionGet();
        return getSnapshotsResponse.getSnapshots();
    }

    @Override
    public Snapshot get(String name) {
        for (SnapshotInfo info : getAllSnapshotInfos()) {
            if (info.name().equals(name)) {
                return snapshotFromInfo(info);
            }
        }
        return null;
    }

    @Override
    public List<Snapshot> getAll() {
        List<SnapshotInfo> infos = getAllSnapshotInfos();
        List<Snapshot> snapshots = new ArrayList<Snapshot>(infos.size());
        for (SnapshotInfo info : infos) {
            Snapshot snapshot = snapshotFromInfo(info);
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Override
    public List<Snapshot> getAll(SnapshotState state) {
        List<Snapshot> snapshots = new ArrayList<Snapshot>();
        for (SnapshotInfo info : getAllSnapshotInfos()) {
            if (getState(info.state()) == state) {
                Snapshot snapshot = snapshotFromInfo(info);
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    @Override
    public boolean restore(Snapshot snapshot, SnapshotRestoreBuilder restore) {
        RestoreSnapshotRequestBuilder builder = new RestoreSnapshotRequestBuilder(client.admin().cluster());
        builder.setRepository(snapshotRepoName)
                .setSnapshot(snapshot.getName());
        if (restore.getIndices() != null) {
            builder.setIndices(restore.getIndices());
        }
        if (restore.getRenamePattern() != null) {
            builder.setRenamePattern(restore.getRenamePattern());
        }
        if (restore.getRenameReplacement() != null) {
            builder.setRenameReplacement(restore.getRenameReplacement());
        }
        RestoreSnapshotResponse response = builder.get();
        return response != null; // RestoreInfo is null if restore is not yet completed
    }

    @Override
    public boolean delete(Snapshot snapshot) {
        DeleteSnapshotRequestBuilder builder = new DeleteSnapshotRequestBuilder(client.admin().cluster());
        builder.setRepository(snapshotRepoName).setSnapshot(snapshot.getName());
        DeleteSnapshotResponse response = builder.get();
        return response.isAcknowledged();
    }
}
