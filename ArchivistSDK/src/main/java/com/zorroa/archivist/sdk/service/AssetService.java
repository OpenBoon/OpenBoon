package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.AssetUpdateBuilder;
import com.zorroa.archivist.sdk.domain.Folder;

import java.util.Collection;

public interface AssetService {

    Asset createAsset(AssetBuilder builder);

    Asset get(String id);

    boolean assetExistsByPath(String path);

    boolean assetExistsByPathAfter(String path, long afterTime);

    boolean replaceAsset(AssetBuilder builder);

    boolean updateAsset(String assetId, AssetUpdateBuilder builder);

    boolean select(String assetId, boolean selected);

    void addToFolder(Asset asset, Folder folder);

    void setFolders(Asset asset, Collection<Folder> folders);
}
