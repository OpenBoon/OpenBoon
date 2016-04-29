package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

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
    PermissionDao permissionDao;

    @Autowired
    MessagingService messagingService;

    @Override
    public Asset get(String id) {
        return assetDao.get(id);
    }

    @Override
    public List<Asset> getAll() {
        return assetDao.getAll();
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

        Asset asset = assetDao.get(assetId);
        PermissionSchema permissions = asset.getAttr("permissions", PermissionSchema.class);

        if (!SecurityUtils.hasPermission(permissions.getWrite())) {
            throw new AccessDeniedException("You cannot make changes to this asset.");
        }

        long version = assetDao.update(assetId, builder);
        messagingService.broadcast(new Message(MessageType.ASSET_UPDATE,
                ImmutableMap.of(
                        "assetId", assetId,
                        "version", version,
                        "source", builder)));
        return version;
    }
}
