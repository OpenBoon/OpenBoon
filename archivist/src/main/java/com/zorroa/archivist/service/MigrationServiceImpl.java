package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;
import com.zorroa.archivist.repository.MigrationDao;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chambers on 2/2/16.
 */
@Component
public class MigrationServiceImpl implements MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    MigrationDao migrationDao;

    @Autowired
    Client client;

    @Autowired
    DataSourceTransactionManager transactionManager;

    /**
     * We need these here to ensure we run AFTER flyway.
     */
    @Autowired
    Flyway flyway;

    private static final int BULK_SIZE = 125;
    private static final long BULK_TIMEOUT = 60000;

    @Override
    public void processAll() {
        processMigrations(migrationDao.getAll(), false);
    }

    @Override
    public List<Migration> getAll() {
        return migrationDao.getAll();
    }

    @Override
    public List<Migration> getAll(MigrationType type) {
        return migrationDao.getAll(type);
    }

    @Override
    public void processMigrations(List<Migration> migrations, boolean force) {

        String snapshotName = System.getenv("ARCHIVIST_RESTORE_INDEX");
        if (snapshotName != null) {
            restoreSnapshot(snapshotName);
        }

        /**
         * TODO: Don't let ingests run during migrations.
         */
        logger.info("Processing migrations");
        for (Migration m : migrations) {
            switch (m.getType()) {
                case ElasticSearchIndex:
                    processElasticMigration(m, force);
                    break;
            }
        }
    }

    public void setRefreshInterval(String index, String value) {
        logger.info("setting refresh interval to: {}", value);
        UpdateSettingsRequestBuilder usrb = client.admin().indices()
                .prepareUpdateSettings();
        usrb.setIndices(index);
        usrb.setSettings(ImmutableMap.of("index.refresh_interval", value));
        usrb.get();
    }

    /**
     * Wait for a yellow cluster status before starting a reindex.
     */
    public void waitOnClusterStatus(String index) {
        String status = properties.getString("archivist.index.migrateStatus");
        logger.info("Waiting on {} cluster status for index: {}", status, index);
        if ("green".equals(status)) {
            client.admin().cluster().prepareHealth(index)
                    .setWaitForGreenStatus()
                    .get();
        }
        else {
            client.admin().cluster().prepareHealth(index)
                    .setWaitForYellowStatus()
                    .get();
        }
    }

    /**
     * This method opens the elastic mapping supplied at the given
     * path and compares the embedded version number with the version number
     * we're running.  If the numbers are different, then a migration is
     * kicked off.
     *
     * No Ingests should be scheduled at this time.
     *
     * @param m
     */
    @Override
    public void processElasticMigration(Migration m, boolean force) {

        ElasticMigrationProperties props;
        try {
            props = getLatestVersion(m);
        } catch (IOException e) {
            logger.warn("Failed to migration elastic index, unable to load elastic mapping: {}", m.getPath());
            throw new RuntimeException("Failed to setup ElasticSearch index, ", e);
        }

        final String oldIndex = String.format("%s_%02d", m.getName(), m.getVersion());
        final String newIndex = String.format("%s_%02d", m.getName(), props.getVersion());
        final boolean oldIndexExists = client.admin().indices().prepareExists(oldIndex).get().isExists();
        final boolean newIndexExists = client.admin().indices().prepareExists(newIndex).get().isExists();

        /**
         * If we already have the current version then check for patch versions.
         */
        if (props.getVersion() == m.getVersion() && newIndexExists) {
            logger.info("'{}' mapping V{} is the current version", m.getName(), m.getVersion());
            applyMappingPatches(m);
            return;
        }

        /**
         * If neither index exists then its the first time the index has been created.
         */
        if (!oldIndexExists && !newIndexExists) {
            logger.info("No indexes exist, {} will be created", newIndex);
        }
        else if (m.getVersion() > props.getVersion()) {
            logger.warn("Version {} is higher than version {}, not downgrading.",
                    m.getVersion(), props.getVersion());
            return;
        }

        /**
         * For unit tests, suspend the unit test transaction and execute the update
         * in a separate transaction, that we're not starting at V1 every time.
         */
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(Propagation.REQUIRES_NEW.ordinal());
        boolean updated = tt.execute(transactionStatus -> {
            boolean result = migrationDao.setVersion(m, props.getVersion(), props.getPatch());
            return result;
        });

        if (!updated) {
            logger.warn("Could not update migration record to version: " + props.getVersion() +
                    ", already set to that version.");
        }

        if (newIndexExists) {
            logger.warn("New index '{}' already exists, may not be latest version", newIndex);
            client.admin().indices().prepareOpen(newIndex).get();
            return;
        }

        /**
         * Wait on the cluster status to at least be yellow.
         */
        waitOnClusterStatus(oldIndex);

        logger.info("Processing migration: {}, path={}, force={}", m.getName(), m.getPath(), force);
        client.admin()
                .indices()
                .prepareCreate(newIndex)
                .setSource(props.getMapping())
                .get();

        /**
         * If there is an old index and the new index is a different name than
         * the old index, AND the new index props say we must reindex, then
         * do a reindex.
         */
        if (oldIndexExists && !oldIndex.equals(newIndex) && props.isReindex()) {
            /**
             * Setup a bulk processor
             */
            BulkProcessor bulkProcessor = BulkProcessor.builder(
                    client,
                    new BulkProcessor.Listener() {
                        @Override
                        public void beforeBulk(long executionId,
                                               BulkRequest request) {
                            logger.info("Executing {} bulk index requests", request.numberOfActions());
                        }

                        @Override
                        public void afterBulk(long executionId,
                                              BulkRequest request,
                                              BulkResponse response) {
                        }

                        @Override
                        public void afterBulk(long executionId,
                                              BulkRequest request,
                                              Throwable failure) {
                            logger.warn("Bulk index failure, ", failure);
                        }
                    })
                    .setBulkActions(BULK_SIZE)
                    .setConcurrentRequests(1)
                    .build();

            /**
             * Now scan/scroll over everything and copy from new to old.
             */
            setRefreshInterval(newIndex, "-1");
            try {
                SearchResponse scrollResp = client.prepareSearch(oldIndex)
                        .setSearchType(SearchType.SCAN)
                        .setScroll(new TimeValue(BULK_TIMEOUT))
                        .addSort("_doc", SortOrder.ASC)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setSize(BULK_SIZE).execute().actionGet();

                while (true) {
                    for (SearchHit hit : scrollResp.getHits().getHits()) {
                        bulkProcessor.add(client.prepareIndex(
                                newIndex, hit.getType(), hit.getId()).setSource(hit.source()).request());
                    }

                    scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
                            new TimeValue(BULK_TIMEOUT)).execute().actionGet();
                    if (scrollResp.getHits().getHits().length == 0) {
                        break;
                    }
                }
            } finally {
                setRefreshInterval(newIndex, "1s");
            }
        }
        /**
         * Flip the alias.
         *
         * The index we're removing it from might not exist for some reason.
         */
        try {
            IndicesAliasesRequestBuilder req = client.admin().indices().prepareAliases();
            logger.info("old index: {} exists: {}, removing alias.", oldIndex, oldIndexExists);
            if (oldIndexExists) {
                req.removeAlias(oldIndex, m.getName());
            }
            req.addAlias(newIndex, m.getName()).execute().actionGet();
        } catch (ElasticsearchException e) {
            logger.warn("Could not remove alias from {}, error was: '{}'. (this is ok)", oldIndex, e.getMessage());
        }

        /**
         * Now close the old index so we don't waste time on it.
         */
        if (oldIndexExists) {
            logger.info("Closing index: {}", oldIndex);
            client.admin().indices().prepareClose(oldIndex).get();
        }
    }


    private static final Pattern MAPPING_NAMING_CONV = Pattern.compile("^V(\\d+)__(.*?).json$");

    private static final Pattern MAPPING_PATCH_CONV = Pattern.compile("^V(\\d+)\\.(\\d)+__(.*?).json$");

    public void applyMappingPatches(Migration m) {
        Map<Integer, Map<String,Object>> mappingPatches = Maps.newHashMap();
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
            Resource[] resources = resolver.getResources(m.getPath());
            for (Resource resource : resources) {
                Matcher matcher = MAPPING_PATCH_CONV.matcher(resource.getFilename());
                if (!matcher.matches()) {
                    continue;
                }

                int majorVersion = Integer.valueOf(matcher.group(1));
                int patchVersion = Integer.valueOf(matcher.group(2));

                if (majorVersion != m.getVersion()) {
                    continue;
                }

                // Check if its been already applied
                if (patchVersion <= m.getPatch()) {
                    logger.info("Patch {}.{} has already been applied", majorVersion, patchVersion);
                    continue;
                }

                mappingPatches.put(patchVersion, Json.Mapper.readValue(resource.getInputStream(),
                        Json.GENERIC_MAP));
            }
        } catch (Exception e) {
            logger.warn("Unable to location elastic migration patch files", e);
            return;
        }

        /**
         * TODO: patches can only be applied to assets right now.
         */
        List<Integer> keys = Lists.newArrayList(mappingPatches.keySet());
        Collections.sort(keys);
        logger.info("Applying {} patch versions to {}", keys.size(), m);
        for (Integer key: keys) {
            try {
                client.admin()
                        .indices()
                        .preparePutMapping(m.getName())
                        .setType("asset")
                        .setSource(mappingPatches.get(key))
                        .get();
                migrationDao.setPatch(m, key);
            } catch (Exception e) {
                logger.warn("Failed to apply elastic mapping patch version: {}", m, e);
            }
        }
    }

    public ElasticMigrationProperties getLatestVersion(Migration m) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources = resolver.getResources(m.getPath());
        List<ElasticMigrationProperties> allVersions = Lists.newArrayList();
        for (Resource resource : resources) {

            Matcher matcher = MAPPING_NAMING_CONV.matcher(resource.getFilename());
            if (!matcher.matches()) {
                continue;
            }

            ElasticMigrationProperties emp = new ElasticMigrationProperties();
            int version = Integer.valueOf(matcher.group(1));
            logger.info("Found embedded mapping in {} version {}", m.getPath(), version);

            Map<String, Object> mapping = Json.Mapper.readValue(resource.getInputStream(), Json.GENERIC_MAP);
            emp.setMapping(mapping);
            emp.setVersion(version);

            /**
             * Only versions with the migration header are added.
             */
            if (mapping.containsKey("migration")) {
                Map<String, Object> props = Json.Mapper.convertValue(mapping.get("migration"),
                        new TypeReference<Map<String, Object>>(){});

                Boolean reindex = (Boolean) props.get("reindex");
                if (reindex != null) {
                    emp.setReindex(reindex);
                }

                Integer patch = (Integer) props.get("patch");
                if (patch != null) {
                    emp.setPatch(patch);
                }

                allVersions.add(emp);
            }
            else {
                logger.warn("Unable to find version/migration info in mapping {}", resource.getFilename());
            }
        }

        if (allVersions.isEmpty()) {
            throw new IOException("Failed to find latest mapping for migration " + m.getPath());
        }

        Collections.sort(allVersions, (o1, o2) -> Integer.compare(o2.getVersion(), o1.getVersion()));
        ElasticMigrationProperties result = allVersions.get(0);
        logger.info("latest '{}' mapping ver: {} (source='{}')", m.getName(), result.getVersion(), m.getPath());
        return result;
    }

    private void restoreSnapshot(String name) {
        /*
         * Additional info:
         * https://www.elastic.co/guide/en/elasticsearch/reference/2.4/modules-snapshots.html
         */

        File file =  properties.getPath("archivist.path.backups")
                .resolve("index").resolve("snap-" + name + ".dat").toFile();

        boolean snapshotExists = file.exists();
        if (!snapshotExists) {
            throw new RuntimeException("Invalid snapshot name " + name + ", a snapshot file does" +
                    "not exist: " + file.toString());
        }

        logger.warn("Restoring from snapshot: {}", name);
        try {
            client.admin().indices().prepareClose("_all").get();
        } catch (Exception e) {
            logger.warn("Failed to close all indexes, this is probably OK", e);
        }

        try {
            client.admin().cluster().prepareRestoreSnapshot("archivist", name).get();
        } catch (RepositoryMissingException rme) {
            /*
             * If the index folder was deleted then we lost the snapshot, so we make an entry
             * for the same one and then try to restore it.
             */
            /**
             * TODO: I think we have to iterate through all snapshots and re-create
             * records for them.
             */
            try {
                logger.info("Recreating snapshot record for: {}", name);
                client.admin().cluster().prepareCreateSnapshot("archivist", name).get();

                logger.warn("Restoring from snapshot: {}", name);
                client.admin().cluster().prepareRestoreSnapshot("archivist", name).get();
            } catch (Exception e) {
                logger.warn("Failed to recover and restore snapshot: ", e);
            }
        }
    }

    private boolean snapshotExists(String name) {
        File file =  properties.getPath("archivist.path.backups")
                .resolve("index").resolve(name).toFile();
        boolean exists = file.exists();
        logger.info("Elastic snapshot exists: {}, '{}'", exists, file);
        return exists;
    }

    private static class ElasticMigrationProperties {
        private int version = 1;
        private boolean reindex = false;
        private boolean reingest = false;
        private int patch = 0;
        private Map<String, Object> mapping;

        public int getVersion() {
            return version;
        }

        public ElasticMigrationProperties setVersion(int version) {
            this.version = version;
            return this;
        }

        public ElasticMigrationProperties incrementVersion(int version, Map<String, Object> mapping) {
            if (version > this.version) {
                this.version = version;
                this.mapping = mapping;
            }
            return this;
        }

        public boolean isReindex() {
            return reindex;
        }

        public ElasticMigrationProperties setReindex(boolean reindex) {
            this.reindex = reindex;
            return this;
        }

        public boolean isReingest() {
            return reingest;
        }

        public ElasticMigrationProperties setReingest(boolean reingest) {
            this.reingest = reingest;
            return this;
        }

        public Map<String, Object> getMapping() {
            return mapping;
        }

        public ElasticMigrationProperties setMapping(Map<String, Object> mapping) {
            this.mapping = mapping;
            return this;
        }

        public int getPatch() {
            return patch;
        }

        public ElasticMigrationProperties setPatch(int patch) {
            this.patch = patch;
            return this;
        }
    }
}
