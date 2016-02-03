package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;
import com.zorroa.archivist.repository.MigrationDao;
import com.zorroa.archivist.sdk.exception.ArchivistException;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.ElasticsearchException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 2/2/16.
 */
@Component
public class MigrationServiceImpl implements MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

    @Value("${archivist.index.alias}")
    String alias;

    @Autowired
    MigrationDao migrationDao;

    @Autowired
    Client client;

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

        Map<String, Object> mapping = null;
        try {
            ClassPathResource resource = new ClassPathResource(m.getPath());
            mapping = Json.Mapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, Object>>() {});

        } catch (IOException e) {
            logger.warn("Failed to migration elastic index, unable to load elastic mapping: {}", m.getPath());
            throw new ArchivistException("Failed to setup ElasticSearch index, ", e);
        }

        /**
         * Assume the mapping version is 1.  This should handle people updating from 0.15 to 0.17
         */
        ElasticMigrationProperties props = new ElasticMigrationProperties();
        if (mapping.containsKey("version")) {
            props.setVersion((int) mapping.get("version"));
        }
        else if (mapping.containsKey("migration")) {
            props = Json.Mapper.convertValue(
                    mapping.get("migration"), ElasticMigrationProperties.class);
        }
        else {
            logger.warn("Unable to find version/migration info in mapping.");
        }

        final String oldIndex = String.format("%s_%02d", alias, m.getVersion());
        final String newIndex = String.format("%s_%02d", alias, props.getVersion());
        final boolean oldIndexExists = client.admin().indices().prepareExists(oldIndex).get().isExists();
        final boolean newIndexExists = client.admin().indices().prepareExists(newIndex).get().isExists();

        /**
         * If neither index exists then its the first time the index has been created.
         */
        if (!oldIndexExists && !newIndexExists) {
            force = true;
        }

        if (!force) {
            if (props.getVersion() == m.getVersion()) {
                return;
            }

            if (!migrationDao.setVersion(m, props.getVersion())) {
                return;
            }
        }

        logger.info("Processing migration: {}, path={}, force={}", m.getName(), m.getPath(), force);
        client.admin()
                .indices()
                .prepareCreate(newIndex)
                .setSource(mapping)
                .get();

        /**
         * Note that, we're flipping the alias first.
         *
         * First we try to remove
         * the old alias.  The index we're removing it from might not exist
         * for some reason.
         */
        logger.info("Removing alias from: {}", oldIndex);
        try {
            client.admin().indices().prepareAliases()
                    .removeAlias(oldIndex, alias)
                    .execute().actionGet();
        } catch (ElasticsearchException e) {
            logger.warn("Could not remove alias from {}, error was: '{}'. (this is ok)", oldIndex, e.getMessage());
        }

        /**
         * Now add the alias to the new index.  If this fails then we have
         * a critical error.
         */
        logger.info("Adding alias to: {}", newIndex);
        try {
            client.admin().indices().prepareAliases()
                    .addAlias(newIndex, alias)
                    .execute().actionGet();
        } catch (ElasticsearchException e) {
            logger.error("Failed to add alias to {}", newIndex, e);
            throw e;
        }

        /**
         * If there is no old index or the index versions are the same, then just
         * return.
         */
        if ((!oldIndexExists || oldIndex == newIndex) && props.isReindex())  {
            return;
        }

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

        /**
         * Now close the old index so we don't waste time on it.
         */
        client.admin().indices().prepareClose(oldIndex);
    }

    private static class ElasticMigrationProperties {
        private int version = 1;
        private boolean reindex = false;
        private boolean reingest = false;

        public int getVersion() {
            return version;
        }

        public ElasticMigrationProperties setVersion(int version) {
            this.version = version;
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
    }
}
