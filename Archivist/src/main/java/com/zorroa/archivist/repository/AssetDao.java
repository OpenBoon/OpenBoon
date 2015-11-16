package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetUpdateBuilder;
import com.zorroa.archivist.sdk.domain.Export;

import java.util.List;

public interface AssetDao {

    Asset create(AssetBuilder builder);

    Asset get(String id);

    List<Asset> getAll();

    boolean existsByPath(String path);

    boolean existsByPathAfter(String path, long afterTime);

    boolean replace(AssetBuilder builder);

    boolean update(String assetId, AssetUpdateBuilder builder);

    void addToExport(Asset asset, Export export);
}
