package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.CommandDao;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.schema.LinkSchema;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @author chambers
 *
 */
@Component
public class AssetServiceImpl implements AssetService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AssetServiceImpl.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    CommandDao commandDao;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    DyHierarchyService dyHierarchyService;

    @Autowired
    TaxonomyService taxonomyService;

    @Autowired
    EventLogService logService;

    @Autowired
    SearchService searchService;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    UserService userService;

    @Autowired
    JobService jobService;

    @Autowired
    Client client;

    PermissionSchema defaultPerms = new PermissionSchema();

    Executor executor = Executors.newFixedThreadPool(4);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        setDefaultPermissions();
    }

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
    public Document index(Document doc) {
        AssetIndexResult result = index(new AssetIndexSpec(doc));
        if (result.getAssetIds().size() == 1) {
            if (doc.getType().equals("asset")) {
                return assetDao.get(result.getAssetIds().get(0));
            }
            else {
                /**
                 * Child types require the parent ID to get them.
                 */
                return assetDao.get(result.getAssetIds().get(0),
                        doc.getType(), doc.getParentId());
            }
        }
        else {
            throw new ArchivistWriteException("Failed to index asset." +
                    Json.serializeToString(result.getLogs()));
        }
    }

    /**
     * Namespaces that are only populated via the API.  IF people manipulate these
     * wrong via the asset API then it would corrupt the asset.
     */
    private static final Set<String> NS_PROTECTED_API = ImmutableSet.of(
            "permissions", "zorroa", "links");

    private static final Set<String> NS_ELEMENT_REMOVE = ImmutableSet.of(
            "source", "origin");

    @Override
    public AssetIndexResult index(AssetIndexSpec spec) {

        /**
         * Clear out any protected name spaces, this lets us ensure people
         * can't manipulate them with the attr API.
         *
         * There is no guarantee the document is the full document, so we can't
         * modify the permissions/links right here since the might not exist,
         * and if they do exist we'll remove them so they don't overwrite
         * the proper value.
         */
        for (Document source: spec.getSources()) {

            /**
             * Remove parts protected by API.
             */
            NS_PROTECTED_API.forEach(n->source.removeAttr(n));

            /**
             * For elements we remove all the stuff we don't need, including
             * the file.
             */
            if (source.getType().equals("element")) {
                final String path = source.getAttr("source.path", String.class);
                executor.execute(() -> {
                    try {
                        Files.deleteIfExists(Paths.get(path));
                    } catch (Exception e) {
                        logger.warn("Failed to remove element: {}", path);
                    }
                });

                NS_ELEMENT_REMOVE.forEach(n -> source.removeAttr(n));
                source.setReplace(true);
                continue;
            }

            Map<String, Object> protectedValues =
                    assetDao.getProtectedFields(source.getId());

            PermissionSchema perms = Json.Mapper.convertValue(
                    protectedValues.getOrDefault("permissions", ImmutableMap.of()), PermissionSchema.class);

            if (source.getPermissions()!= null) {
                for (Map.Entry<String, Integer> entry : source.getPermissions().entrySet()) {
                    try {
                        Permission perm = userService.getPermission(entry.getKey());
                        if ((entry.getValue() & 1) == 1) {
                            perms.addToRead(perm.getId());
                        }
                        else {
                            perms.removeFromRead(perm.getId());
                        }

                        if ((entry.getValue() & 2) == 2) {
                            perms.addToWrite(perm.getId());
                        }
                        else {
                            perms.removeFromWrite(perm.getId());
                        }

                        if ((entry.getValue() & 4) == 4) {
                            perms.addToExport(perm.getId());
                        }
                        else {
                            perms.removeFromExport(perm.getId());
                        }
                    } catch (Exception e) {
                        logger.warn("Permission not found: {}", entry.getKey());
                    }
                }
                source.setAttr("permissions", perms);
            }
            else if (perms.isEmpty()) {
                /**
                 * If the source didn't come with any permissions and the current perms
                 * on the asset are empty, we apply the default permissions.
                 *
                 * If there is no permissions.
                 */
                source.setAttr("permissions", defaultPerms);
                source.setAttr("zorroa.createdDate", new Date());
            }

            if (source.getLinks() != null) {
                LinkSchema links = Json.Mapper.convertValue(
                        protectedValues.getOrDefault("links", ImmutableMap.of()), LinkSchema.class);
                for (Tuple<String, Object> link: source.getLinks()) {
                    links.addLink(link.getLeft(), link.getRight());
                }
                source.setAttr("links", links);
            }
        }

        AssetIndexResult result = assetDao.index(spec.getSources());
        TaskStatsAdder addr = new TaskStatsAdder(result);

        if (spec.getTaskId() != null && spec.getJobId() != null) {
            jobService.incrementStats(new TaskId() {
                @Override
                public Integer getJobId() {
                    return spec.getJobId();
                }

                @Override
                public Integer getTaskId() {
                    return spec.getTaskId();
                }

                @Override
                public Integer getParentTaskId() {
                    return null;
                }
            }, addr);
        }

        if (result.created + result.updated + result.replaced > 0) {

            /**
             * TODO: make these 1 thread pool
             */
            dyHierarchyService.submitGenerateAll(true);
            taxonomyService.runAllAsync();
        }
        return result;
    }

    @Override
    public void removeFields(String id, Set<String> fields) {
        // remove fields from list the can't remove.
        fields.removeAll(NS_PROTECTED_API);
        assetDao.removeFields(id ,fields, false);
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
    public boolean exists(String id) {
        return assetDao.exists(id);
    }

    @Override
    public long update(String assetId, Map<String, Object> attrs) {

        Asset asset = assetDao.get(assetId);
        Set<Integer> write = asset.getAttr("permissions.write", Set.class);

        if (!SecurityUtils.hasPermission(write)) {
            throw new ArchivistWriteException("You cannot make changes to this asset.");
        }

        Map<String, Object> copy = Maps.newHashMap(attrs);
        /**
         * Remove keys which are maintained via other methods.
         */
        NS_PROTECTED_API.forEach(n->copy.remove(n));

        long version = assetDao.update(assetId, copy);
        logService.logAsync(UserLogSpec.build(LogAction.Update, "asset", assetId));
        return version;
    }

    @Override
    public boolean delete(String assetId) {
        return assetDao.delete(assetId);
    }

    @Override
    public void setPermissions(Command command, AssetSearch search, Acl acl) {

        final long totalCount = searchService.count(search);
        if (totalCount == 0) {
            return;
        }

        PermissionSchema add = new PermissionSchema();
        PermissionSchema remove = new PermissionSchema();

        /**
         * Convert the ACL to a PermissionSchema.
         */
        for (AclEntry entry: permissionDao.resolveAcl(acl, false)) {

            if ((entry.getAccess() & 1) != 0) {
                add.addToRead(entry.getPermissionId());
            }
            else {
                remove.addToRead(entry.getPermissionId());
            }

            if ((entry.getAccess() & 2) != 0) {
                add.addToWrite(entry.getPermissionId());
            }
            else {
                remove.addToWrite(entry.getPermissionId());
            }

            if ((entry.getAccess() & 4) != 0) {
                add.addToExport(entry.getPermissionId());
            }
            else {
                remove.addToExport(entry.getPermissionId());
            }
        }

        logger.info("Adding permissions: {}", Json.serializeToString(add));
        logger.info("Removing permissions: {}", Json.serializeToString(remove));

        LongAdder totalSuccess = new LongAdder();
        LongAdder totalFailed = new LongAdder();
        AtomicBoolean error = new AtomicBoolean(false);

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

                        int failureCount = 0;
                        int successCount = 0;
                        for (BulkItemResponse bir: response.getItems()) {
                            if (bir.isFailed()) {
                                logger.warn("update permissions bulk failed: {}",  bir.getFailureMessage());
                                failureCount++;
                            }
                            else {
                                successCount++;
                            }
                        }
                        commandDao.updateProgress(command, totalCount, successCount, failureCount);
                        totalSuccess.add(successCount);
                        totalFailed.add(failureCount);
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable thrown) {
                        error.set(true);
                        logger.warn("Failed to set permissions, ", thrown);
                    }
                })
                .setBulkActions(250)
                .setConcurrentRequests(0)
                .build();

        for (Asset asset: searchService.scanAndScroll(
                search.setFields(new String[] {"permissions"}), 0)) {

            if (command.getState().equals(JobState.Cancelled)) {
                logger.warn("setPermissions was canceled");
                break;
            }

            if (error.get()) {
                logger.warn("Encountered error while setting permissions, exiting");
                break;
            }

            UpdateRequestBuilder update = client.prepareUpdate("archivist", "asset", asset.getId());
            PermissionSchema current = asset.getAttr("permissions", PermissionSchema.class);
            if (current == null) {
                current = new PermissionSchema();
            }

            /**
             * Add all permissions specified by ACL
             */
            current.getRead().addAll(add.getRead());
            current.getWrite().addAll(add.getWrite());
            current.getExport().addAll(add.getExport());

            /**
             * Remove all permissions set to 0 in ACL.
             */
            current.getRead().removeAll(remove.getRead());
            current.getWrite().removeAll(remove.getWrite());
            current.getExport().removeAll(remove.getExport());

            update.setDoc(Json.serializeToString(ImmutableMap.of("permissions", current)));
            bulkProcessor.add(update.request());
        }

        try {
            logger.info("Waiting for bulk permission change to complete on {} assets.", totalCount);
            bulkProcessor.awaitClose(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            logService.log(new UserLogSpec()
                    .setUser(command.getUser())
                    .setMessage("Bulk permission change complete.")
                    .setAction(LogAction.BulkUpdate)
                    .setAttrs(ImmutableMap.of("permissions", add)));
        } catch (InterruptedException e) {
            logService.log(new UserLogSpec()
                    .setUser(command.getUser())
                    .setMessage("Bulk update failure setting permissions on assets, interrupted.")
                    .setAction(LogAction.BulkUpdate));
        }

        logger.info("Bulk permission change complete, total: {} success: {} failed: {}",
                totalCount, totalSuccess.longValue(), totalFailed.longValue());
    }

    @Override
    public Map<String, Object> getMapping() {
        return assetDao.getMapping();
    }

    private void setDefaultPermissions() {
        List<String> defaultReadPerms =
                properties.getList("archivist.security.permissions.defaultRead");
        List<String> defaultWritePerms =
                properties.getList("archivist.security.permissions.defaultWrite");
        List<String> defaultExportPerms =
                properties.getList("archivist.security.permissions.defaultExport");

        for(Permission p: permissionDao.getAll(defaultReadPerms)) {
            defaultPerms.addToRead(p.getId());
        }

        for(Permission p: permissionDao.getAll(defaultWritePerms)) {
            defaultPerms.addToWrite(p.getId());
        }

        for(Permission p: permissionDao.getAll(defaultExportPerms)) {
            defaultPerms.addToExport(p.getId());
        }
    }

}
