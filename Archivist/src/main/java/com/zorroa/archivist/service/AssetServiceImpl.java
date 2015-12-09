package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 *
 * @author chambers
 *
 */
@Component
public class AssetServiceImpl implements AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetServiceImpl.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    RoomService roomService;

    @Override
    public Asset get(String id) {
        return assetDao.get(id);
    }

    @Override
    public Asset createAsset(AssetBuilder builder) {
        return assetDao.create(builder);
    }

    @Override
    public boolean replaceAsset(AssetBuilder builder) {
        return assetDao.replace(builder);
    }

    @Override
    public boolean assetExistsByPath(String path) {
        return assetDao.existsByPath(path);
    }

    @Override
    public boolean assetExistsByPathAfter(String path, long afterTime) {
        return assetDao.existsByPathAfter(path, afterTime);
    }

    @Override
    public long update(String assetId, AssetUpdateBuilder builder) {
        long version = assetDao.update(assetId, builder);
        roomService.sendToActiveRoom(new Message(MessageType.ASSET_UPDATE,
                ImmutableMap.of(
                        "assetId", assetId,
                        "version", version,
                        "source", Json.serializeToString(builder))));
        return version;
    }

    @Override
    public boolean select(String assetId, boolean selected) {
        return assetDao.select(assetId, selected);
    }

    @Override
    public void addToFolder(Asset asset, Folder folder) {
        assetDao.addToFolder(asset, folder);
    }

    @Override
    public void setFolders(Asset asset, Collection<Folder> folders) {
        long version = assetDao.setFolders(asset, folders);

        roomService.sendToActiveRoom(new Message(MessageType.ASSET_UPDATE_FOLDERS,
            ImmutableMap.of(
                    "assetId", asset.getId(),
                    "folders", folders.stream().map(Folder::getId).collect(Collectors.toList()),
                    "version", version)));
    }
}
