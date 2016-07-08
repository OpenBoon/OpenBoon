package com.zorroa.common.repository;

import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.BatchAssetUpsertResult;
import com.zorroa.sdk.domain.Folder;
import com.zorroa.sdk.processor.Source;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface AssetDao {

    Asset get(String id);

    List<Asset> getAll();

    boolean exists(Path path);

    Asset get(Path path);

    long update(String assetId, Map<String, Object> attrs);

    Asset upsert(Source source);

    BatchAssetUpsertResult upsert(List<Source> sources);

    int addToFolder(Folder folder, List<String> assetIds);

    int removeFromFolder(Folder folder, List<String> assetIds);
}
