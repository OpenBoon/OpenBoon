package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.MessagingService;
import com.zorroa.archivist.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

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
    MessagingService messagingService;

    @Override
    public Asset get(String id) {
        return assetDao.get(id);
    }

    @Override
    public Asset upsert(AssetBuilder builder) {
        return assetDao.upsert(builder);
    }

    @Override
    public String upsertAsync(AssetBuilder builder) {
        return assetDao.upsertAsync(builder);
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
        messagingService.broadcast(new Message(MessageType.ASSET_UPDATE,
                ImmutableMap.of(
                        "assetId", assetId,
                        "version", version,
                        "source", builder)));
        return version;
    }

    @Override
    public void addToFolder(Asset asset, Folder folder) {
        if(!folder.getAcl().hasAccess(SecurityUtils.getPermissionIds(), Access.Write)) {
            throw new AccessDeniedException("You cannot make changes to this folder");
        }
        assetDao.addToFolder(asset, folder);

    }

    @Override
    public void removeFromFolder(Asset asset, Folder folder) {
        if(!folder.getAcl().hasAccess(SecurityUtils.getPermissionIds(), Access.Write)) {
            throw new AccessDeniedException("You cannot make changes to this folder");
        }
        assetDao.removeFromFolder(asset, folder);
    }

}
