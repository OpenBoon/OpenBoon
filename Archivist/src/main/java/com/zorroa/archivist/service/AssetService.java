package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetUpdateBuilder;
import com.zorroa.archivist.sdk.AssetBuilder;

public interface AssetService {

    Asset createAsset(AssetBuilder builder);

    boolean assetExistsByPath(String path);

    boolean assetExistsByPathAfter(String path, long afterTime);

    boolean replaceAsset(AssetBuilder builder);

    boolean updateAsset(String assetId, AssetUpdateBuilder builder);
}
