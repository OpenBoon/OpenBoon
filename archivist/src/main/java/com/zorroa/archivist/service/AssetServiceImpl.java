package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.domain.MessageType;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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

    @Autowired
    DyHierarchyService dyHierarchyService;

    @Autowired
    LogService logService;

    @Override
    public Asset get(String id) {
        if (id.startsWith("/")) {
            return get(Paths.get(id));
        }
        else {
            return assetDao.get(id);
        }
    }

    @Override
    public Asset get(Path path) {
        return assetDao.get(path);
    }

    @Override
    public PagedList<Asset> getAll(Paging page) {
        return assetDao.getAll(page);
    }

    @Override
    public Asset index(Source source) {
        return assetDao.index(source);
    }

    @Override
    public AssetIndexResult index(String index, List<Source> sources) {
        AssetIndexResult result =  assetDao.index(index, sources);
        if (result.created + result.updated > 0) {
            dyHierarchyService.submitGenerateAll(false);
        }
        return result;
    }

    @Override
    public AssetIndexResult index(List<Source> sources) {
        AssetIndexResult result =  assetDao.index(sources);
        if (result.created + result.updated > 0) {
            dyHierarchyService.submitGenerateAll(false);
        }
        return result;
    }

    @Override
    public boolean exists(Path path) {
        return assetDao.exists(path);
    }

    @Override
    public long update(String assetId, Map<String, Object> attrs) {

        Asset asset = assetDao.get(assetId);
        PermissionSchema permissions = asset.getAttr("permissions", PermissionSchema.class);

        if (!SecurityUtils.hasPermission(permissions.getWrite())) {
            throw new AccessDeniedException("You cannot make changes to this asset.");
        }

        long version = assetDao.update(assetId, attrs);
        messagingService.broadcast(new Message(MessageType.ASSET_UPDATE,
                ImmutableMap.of(
                        "assetId", assetId,
                        "version", version,
                        "source", attrs)));
        logService.log(LogSpec.build(LogAction.Update, "asset", assetId));
        return version;
    }
}
