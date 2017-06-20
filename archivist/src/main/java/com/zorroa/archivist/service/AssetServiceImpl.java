package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.CommandDao;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.repository.AssetDao;
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
    FilterService filterService;

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
        PermissionSchema perms = source.getAttr("permissions", PermissionSchema.class);
        if (perms == null) {
            filterService.applyPermissionSchema(source);
        }
        else {
            source.removeAttr("permissions");
        }
        Asset asset = assetDao.index(source, link);
        dyHierarchyService.submitGenerateAll(true);
        return asset;
    }

    @Override
    public Asset index(Source source) {
        return index(source, null);
    }

    @Override
    public DocumentIndexResult index(List<Source> sources, LinkSpec link) {
        for (Source source: sources) {
            try {
                /**
                 * If an asset has no permission schema, then run it through the filters.
                 * Otherwise, we remove the permissions from the doc since they cannot
                 * be set from a processor.
                 */
                PermissionSchema perms = source.getAttr("permissions", PermissionSchema.class);
                if (perms == null) {
                    filterService.applyPermissionSchema(source);
                }
                else {
                    source.removeAttr("permissions");
                }
            } catch (Exception e) {
                logger.warn("Asset {} has invalid permission schema.", source.getId());
                source.removeAttr("permissions");
            }
        }

        DocumentIndexResult result =  assetDao.index(sources, link);
        if (result.created + result.updated > 0) {
            dyHierarchyService.submitGenerateAll(true);
            taxonomyService.runAllAsync();
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

        /**
         * Remove permissions which are handled with the setPermissions function.
         */
        try {
            attrs.remove("permissions");
        } catch (java.lang.UnsupportedOperationException e) {
            // ignore immutable maps from unit test.
        }

        long version = assetDao.update(assetId, attrs);
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
        for (AclEntry entry: permissionDao.resolveAcl(acl)) {

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
                    }
                })
                .setBulkActions(100)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
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
}
