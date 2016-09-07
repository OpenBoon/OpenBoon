package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;
import com.zorroa.archivist.repository.MigrationDao;
import com.zorroa.sdk.exception.ArchivistException;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
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
            throw new ArchivistException("Failed to setup ElasticSearch index, ", e);
        }

        final String oldIndex = String.format("%s_%02d", m.getName(), m.getVersion());
        final String newIndex = String.format("%s_%02d", m.getName(), props.getVersion());
        final boolean oldIndexExists = client.admin().indices().prepareExists(oldIndex).get().isExists();
        final boolean newIndexExists = client.admin().indices().prepareExists(newIndex).get().isExists();

        /**
         * If we alread have the current version then return.
         */
        if (props.getVersion() == m.getVersion() && newIndexExists) {
            logger.info("'{}' mapping V{} is the current version", m.getName(), m.getVersion());
            return;
        }

        /**
         * If neither index exists then its the first time the index has been created.
         */
        if (!oldIndexExists && !newIndexExists) {
            logger.info("No indexes exist, {} will be created", newIndex);
        }

        /**
         * For unit tests, suspend the unit test transaction and execute the update
         * in a separate transaction, that we're not starting at V1 every time.
         */
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(Propagation.REQUIRES_NEW.ordinal());
        boolean updated = tt.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus transactionStatus) {
                boolean result = migrationDao.setVersion(m, props.getVersion());
                return result;
            }
        });

        if (!updated) {
            logger.warn("Could not update migration record to version: " + props.getVersion() +
                    ", already set to that version.");
        }

        if (newIndexExists) {
            logger.warn("New index '{}' already exists, may not be latest version", newIndex);
            return;
        }

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
                    .setBulkActions(500)
                    .setBulkSize(new ByteSizeValue(500, ByteSizeUnit.MB))
                    .setFlushInterval(TimeValue.timeValueSeconds(10))
                    .setConcurrentRequests(1)
                    .build();

            /**
             * Now scan/scroll over everything and copy from new to old.
             */
            SearchResponse scrollResp = client.prepareSearch(oldIndex)
                    .setSearchType(SearchType.SCAN)
                    .setScroll(new TimeValue(60000))
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSize(100).execute().actionGet();

            while (true) {
                for (SearchHit hit : scrollResp.getHits().getHits()) {
                    bulkProcessor.add(client.prepareIndex(
                            newIndex, hit.getType(), hit.getId()).setSource(hit.source()).request());
                }

                scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
                        new TimeValue(60000)).execute().actionGet();
                if (scrollResp.getHits().getHits().length == 0) {
                    break;
                }
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
        client.admin().indices().prepareClose(oldIndex);
    }


    private static final Pattern MAPPING_NAMING_CONV = Pattern.compile("^V(\\d+)__(.*?).json$");

    public ElasticMigrationProperties getLatestVersion(Migration m) throws IOException {
        ElasticMigrationProperties result = new ElasticMigrationProperties();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources = resolver.getResources(m.getPath());
        for (Resource resource : resources) {

            Matcher matcher = MAPPING_NAMING_CONV.matcher(resource.getFilename());
            if (!matcher.matches()) {
                logger.warn("'{}' is not using the proper naming convention.", resource.getFilename());
                continue;
            }

            int version = Integer.valueOf(matcher.group(1));
            Map<String, Object> mapping = Json.Mapper.readValue(resource.getInputStream(), Json.GENERIC_MAP);

            result.setMapping(mapping);
            result.setVersion(version);

            if (mapping.containsKey("migration")) {
                Map<String, Boolean> props = Json.Mapper.convertValue(mapping.get("migration"),
                        new TypeReference<Map<String, Boolean>>(){});

                if (props.get("reindex")) {
                    result.setReindex(true);
                }
                if (props.get("reingest")) {
                    result.setReingest(true);
                }
            }
            else {
                logger.warn("Unable to find version/migration info in mapping {}", resource.getFilename());
            }
        }

        logger.info("latest '{}' mapping ver: {} (source='{}')", m.getName(), result.getVersion(), m.getPath());
        return result;
    }

    private static class ElasticMigrationProperties {
        private int version = 1;
        private boolean reindex = false;
        private boolean reingest = false;
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
    }
}
