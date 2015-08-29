package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.sdk.AssetBuilder;

import java.util.List;

public interface AssetDao {

    Asset create(AssetBuilder builder);

    Asset get(String id);

    List<Asset> getAll();

    boolean existsByPath(String path);

    boolean existsByPathAfter(String path, long afterTime);

    boolean replace(AssetBuilder builder);

}
