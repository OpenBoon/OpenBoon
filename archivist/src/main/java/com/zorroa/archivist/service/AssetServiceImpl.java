package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public PagedList<Asset> getAll(Pager page) {
        return assetDao.getAll(page);
    }

    @Override
    public Asset index(Source source, LinkSpec link) {
        return assetDao.index(source, link);
    }

    @Override
    public Asset index(Source source) {
        return index(source, null);
    }

    @Override
    public AssetIndexResult index(List<Source> sources, LinkSpec link) {
        AssetIndexResult result =  assetDao.index(sources, link);
        if (result.created + result.updated > 0) {
            dyHierarchyService.submitGenerateAll(false);
        }
        return result;
    }

    public AssetIndexResult index(List<Source> sources) {
        return index(sources, null);
    }

    @Override
    public Map<String, List<Object>> removeLink(String type, String value, List<String> assets) {
        return assetDao.removeLink(type, value, assets);
    }

    @Override
    public Map<String, List<Object>> appendLink(String type, String value, List<String> assets) {
        return assetDao.appendLink(type, value, assets);
    }

    @Override
    public boolean exists(Path path) {
        return assetDao.exists(path);
    }

    @Override
    public long update(String assetId, Map<String, Object> attrs) {

        Asset asset = assetDao.get(assetId);
        Set<Integer> write = asset.getAttr("permissions.write", Set.class);
        if (!SecurityUtils.hasPermission(write)) {
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
