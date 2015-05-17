package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;

public interface AssetDao {

    String create(AssetBuilder builder);

    Asset get(String id);

    List<Asset> getAll();

}
