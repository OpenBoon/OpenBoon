package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.*;

import java.util.Collection;
import java.util.List;

public interface AssetDao {

    Asset create(AssetBuilder builder);

    Asset get(String id);

    List<Asset> getAll();

    boolean existsByPath(String path);

    boolean existsByPathAfter(String path, long afterTime);

    boolean replace(AssetBuilder builder);

    void addToFolder(Asset asset, Folder folder);

    long setFolders(Asset asset, Collection<Folder> folders);

    long update(String assetId, AssetUpdateBuilder builder);

    boolean select(String assetId, boolean selected);

    void addToExport(Asset asset, Export export);
}
