package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;

public interface AssetDao {

    Asset create(AssetBuilder builder);

    Asset get(String id);

    List<Asset> getAll();

    boolean existsByPath(String path);

    void fastCreate(AssetBuilder builder);

}
