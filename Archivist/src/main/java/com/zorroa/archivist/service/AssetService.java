package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;

public interface AssetService {

    Asset createAsset(AssetBuilder builder);

    boolean assetExistsByPath(String path);

    boolean fastCreateAsset(AssetBuilder builder);

}
