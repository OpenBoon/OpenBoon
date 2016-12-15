package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.zorroa.archivist.SecureSingleThreadExecutor;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

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

    @Autowired
    SearchService searchService;

    @Autowired
    Client client;

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
    public DocumentIndexResult index(List<Source> sources, LinkSpec link) {

        DocumentIndexResult result =  assetDao.index(sources, link);
        if (result.created + result.updated > 0) {
            dyHierarchyService.submitGenerateAll(false);
        }
        return result;
    }

    public DocumentIndexResult index(List<Source> sources) {
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
            throw new ArchivistWriteException("You cannot make changes to this asset.");
        }

        /**
         * Remove permissions which are handled with the setPermissions function.
         */
        try {
            attrs.remove("permissions");
        } catch (java.lang.UnsupportedOperationException e) {
            // ignore immutable maps from unit test.
        }

        long version = assetDao.update(assetId, attrs);
        messagingService.broadcast(new Message(MessageType.ASSET_UPDATE,
                ImmutableMap.of(
                        "assetId", assetId,
                        "version", version,
                        "source", attrs)));
        logService.logAsync(LogSpec.build(LogAction.Update, "asset", assetId));
        return version;
    }

    private final Executor batchExecutor = SecureSingleThreadExecutor.singleThreadExecutor();


    /**
     * Updates the assets for a given search with the given permissions.  Only
     * 1 thread is allowed to be updating permissions at a time, so we don't
     * get conflicts if people submit to conflicting operations.
     *
     * @param search
     * @param acl
     */
    @Override
    public void setPermissionsAsync(AssetSearch search, Acl acl) {
        batchExecutor.execute(() -> setPermissions(search, acl));
    }

    @Override
    public void setPermissions(AssetSearch search, Acl acl) {

        if (!SecurityUtils.hasPermission("group::manager", "group::share", "group::administrator")) {
            throw new ArchivistWriteException("You dot not have the privileges to share assets.");
        }

        PermissionSchema schema = new PermissionSchema();
        Set<Integer> remove = Sets.newHashSet();

        /**
         * Convert the ACL to a PermissionSchema.
         */
        for (AclEntry entry: permissionDao.resolveAcl(acl)) {

            if ((entry.getAccess() & 1) != 0) {
                schema.addToRead(entry.getPermissionId());
            }

            if ((entry.getAccess() & 2) != 0) {
                schema.addToWrite(entry.getPermissionId());
            }

            if ((entry.getAccess() & 4) != 0) {
                schema.addToExport(entry.getPermissionId());
            }

            if (entry.getAccess() == 0) {
                remove.add(entry.getPermissionId());
            }
        }

        final LongAdder success = new LongAdder();
        final LongAdder failure = new LongAdder();
        final User user = SecurityUtils.getUser();

        logger.info("{} Setting permissions: {}", user.getUsername(), Json.serializeToString(schema));
        logger.info("{} Removing permissions: {}", user.getUsername(), Json.serializeToString(remove));

        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client,
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {

                        for (BulkItemResponse bir: response.getItems()) {
                            if (bir.isFailed()) {
                                failure.increment();
                                logService.log(LogSpec.build(LogAction.Update, bir.getType(), bir.getId())
                                        .setMessage("Update permissions failed," + bir.getFailureMessage())
                                        .setUser(user));
                            }
                            else {
                                success.increment();
                                logService.log(LogSpec.build(LogAction.Update, bir.getType(), bir.getId())
                                        .setMessage("Update permissions")
                                        .setUser(user));
                            }
                        }
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable thrown) {
                        logService.log(new LogSpec()
                                .setAction(LogAction.Update)
                                .setType("asset")
                                .setMessage("Update permissions failed " + thrown.getMessage())
                                .setUser(user));
                    }
                })
                .setBulkActions(5000)
                .setFlushInterval(TimeValue.timeValueSeconds(5))
                .setConcurrentRequests(1)
                .build();

        for (Asset asset: searchService.scanAndScroll(
                search.setFields(new String[] {"permissions"}), 0)) {

            UpdateRequestBuilder update = client.prepareUpdate("archivist", "asset", asset.getId());
            PermissionSchema current = asset.getAttr("permissions", PermissionSchema.class);
            if (current == null) {
                current = new PermissionSchema();
            }

            /**
             * Add all permissions specified by ACL
             */
            current.getRead().addAll(schema.getRead());
            current.getWrite().addAll(schema.getWrite());
            current.getExport().addAll(schema.getExport());

            /**
             * Remove all permissions set to 0 in ACL.
             */
            current.getRead().removeAll(remove);
            current.getWrite().removeAll(remove);
            current.getExport().removeAll(remove);

            update.setDoc(Json.serializeToString(ImmutableMap.of("permissions", current)));
            bulkProcessor.add(update.request());
        }

        try {
            bulkProcessor.awaitClose(1, TimeUnit.HOURS);
            logService.log(new LogSpec()
                    .setUser(user)
                    .setMessage("Bulk update set permissions success: " + success.longValue() + ", failed: " + failure.longValue())
                    .setAction(LogAction.BulkUpdate)
                    .setAttrs(ImmutableMap.of("permissions", schema)));
        } catch (InterruptedException e) {
            logService.log(new LogSpec()
                    .setUser(user)
                    .setMessage("Bulk update failure setting permissions on assets, interrupted.")
                    .setAction(LogAction.BulkUpdate));
        }
    }
}
