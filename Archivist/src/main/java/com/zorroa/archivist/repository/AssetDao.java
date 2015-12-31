package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.*;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateResponse;

import java.util.Collection;
import java.util.List;

public interface AssetDao {

    Asset upsert(AssetBuilder builder);

    String upsertAsync(AssetBuilder builder);

    String upsertAsync(AssetBuilder builder, ActionListener<UpdateResponse> listener);

    Asset get(String id);

    List<Asset> getAll();

    boolean existsByPath(String path);

    Asset getByPath(String path);

    boolean existsByPathAfter(String path, long afterTime);

    void addToFolder(Asset asset, Folder folder);

    void removeFromFolder(Asset asset, Folder folder);

    @Deprecated
    long setFolders(Asset asset, Collection<Folder> folders);

    long update(String assetId, AssetUpdateBuilder builder);

    BulkResponse bulkUpsert(List<AssetBuilder> builders);

    void addToExport(Asset asset, Export export);

    void refresh();
}
