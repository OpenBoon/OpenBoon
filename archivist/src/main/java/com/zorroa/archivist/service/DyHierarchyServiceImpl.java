package com.zorroa.archivist.service;


import com.google.common.collect.Queues;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.DyHierarchyDao;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.domain.AssetFieldRange;
import com.zorroa.sdk.domain.AssetFieldTerms;
import com.zorroa.sdk.domain.AssetSearch;
import com.zorroa.sdk.domain.Tuple;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DyHierarchyServiceImpl is repsonsible for the generation of dynamic hierarchies.
 */
@Service
public class DyHierarchyServiceImpl implements DyHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(DyHierarchyServiceImpl.class);

    @Autowired
    DyHierarchyDao dyHierarchyDao;

    @Autowired
    FolderService folderService;

    @Autowired
    Client client;

    @Autowired
    TransactionEventManager transactionEventManager;

    private final AtomicLong runAllTimer = new AtomicLong(System.currentTimeMillis());

    private final ExecutorService dyhiExecute = Executors.newFixedThreadPool(2);

    @Override
    @Transactional
    public boolean update(DyHierarchy dyhi, DyHierarchySpec spec) {
        Folder folder = folderService.get(spec.getFolderId());
        if (!dyHierarchyDao.update(dyhi.getId(), spec)) {
            return false;
        }

        if (dyHierarchyDao.setWorking(dyhi, false)) {
            logger.warn("An existing DyHierarchy {} was running, stopping");
        }

        /**
         * Queue up an event where after this transaction commits
         * we wait until any old folders are cleared out, then
         * the new folders are generated.
         */
        transactionEventManager.afterCommitSync(() -> {
            try {
                while(true) {
                    int count = folderService.count(dyhi);
                    if (count == 0) {
                        submitGenerate(dyhi);
                        return;
                    }
                    Thread.sleep(2500);
                }

            } catch (InterruptedException e) {
                return;
            }
        });

        return true;
    }

    @Override
    @Transactional
    public DyHierarchy create(DyHierarchySpec spec) {
        Folder folder = folderService.get(spec.getFolderId());
        DyHierarchy dyhi = dyHierarchyDao.create(spec);
        folderService.setDyHierarchyRoot(folder, true);

        if (ArchivistConfiguration.unittest) {
            generate(dyhi);
        }
        else {
            transactionEventManager.afterCommitSync(()
                    -> submitGenerate(dyhi));
        }
        return dyhi;
    }

    @Override
    public void generateAll() {
        for (DyHierarchy dyhi: dyHierarchyDao.getAll()) {
            generate(dyhi);
        }
    }

    @Override
    public void submitGenerateAll(boolean force) {
        /**
         * If there was already a generateAll submitted, no need
         * to submit agin.
         */
        if (!force && System.currentTimeMillis() - runAllTimer.get() < 5000) {
            return;
        }
        if (force) {
            ElasticClientUtils.refreshIndex(client);
        }
        runAllTimer.set(System.currentTimeMillis());
        for (DyHierarchy dyhi: dyHierarchyDao.getAll()) {
            dyhiExecute.submit(() -> generate(dyhi));
        }
    }

    @Override
    public Future<Integer> submitGenerate(DyHierarchy dyhi) {
       return dyhiExecute.submit(() -> generate(dyhi));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public int generate(DyHierarchy dyhi) {
        if (dyhi.getLevels() == null || dyhi.getLevels().isEmpty()) {
            return 0;
        }

        if (!dyHierarchyDao.setWorking(dyhi, true)) {
            logger.warn("DyHi {} already running, skipping.", dyhi);
            return 0;
        }

        /**
         * TODO: allow some custom search options here, for example, maybe you
         * want to agg for the last 24 hours.
         */
        try {
            SearchRequestBuilder srb = client.prepareSearch()
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSize(0);

            /**
             * Build a nested list of Elastic aggregations.
             */
            AggregationBuilder terms = elasticAggregationBuilder(dyhi.getLevels().get(0), 0);
            srb.addAggregation(terms);
            for (int i = 1; i < dyhi.getLevels().size(); i++) {
                AggregationBuilder sub = elasticAggregationBuilder(dyhi.getLevel(i), i);
                terms.subAggregation(sub);
                terms = sub;
            }

            /**
             * Breadth first walk of the aggregations which creates the folders as it goes.
             */
            FolderStack folders = new FolderStack(folderService.get(dyhi.getFolderId()), dyhi);
            Queue<Tuple<Aggregations, Integer>> queue = Queues.newArrayDeque();
            queue.add(new Tuple(srb.get().getAggregations(), 0));
            createDynamicHierarchy(queue, folders);

            logger.info("{} created {} folders", dyhi, folders.count);
            return folders.count;
        }
        finally {

            if (!dyHierarchyDao.isWorking(dyhi)) {
                /*
                 * If we get here and the dyhi is set to not working,then
                 * that means someone deleted it while it was running.
                 * That means we're responsible for deleting the folders.
                 */
                logger.warn("The dynamic hierarchy {} was stopped, removing all folders.", dyhi);
                folderService.deleteAll(dyhi);
            }
            else {
                dyHierarchyDao.setWorking(dyhi, false);
            }
        }
    }

    public AggregationBuilder elasticAggregationBuilder(DyHierarchyLevel level, int idx) {

        switch(level.getType()) {
            case Term:
                TermsBuilder terms = AggregationBuilders.terms(String.valueOf(idx));
                terms.field(level.getField());
                return terms;
            case Date:
                DateHistogramBuilder date = AggregationBuilders.dateHistogram(String.valueOf(idx));
                date.field(level.getField());
                date.interval(new DateHistogramInterval((String) level.getOptions().getOrDefault("interval", "1y")));
                date.format((String) level.getOptions().getOrDefault("format", "y"));
                return date;
        }

        return null;
    }

    /**
     * Breadth first walk of aggregations result which builds
     * a tree structure which we'll use create folders.
     *
     * @param queue
     */
    private void createDynamicHierarchy(Queue<Tuple<Aggregations, Integer>> queue, FolderStack folders) {
        /*
         * A fast in memory check.
         */
        if (!dyHierarchyDao.isWorking(folders.dyhi)) {
            return;
        }

        Tuple<Aggregations, Integer> item = queue.poll();
        Aggregations aggs = item.getLeft();
        int depth = item.getRight();
        if (aggs == null) {
            return;
        }

        MultiBucketsAggregation terms = aggs.get(String.valueOf(depth));
        if (terms == null) {
            return;
        }

        DyHierarchyLevel level = folders.dyhi.getLevel(depth);

        Collection<? extends MultiBucketsAggregation.Bucket> buckets = terms.getBuckets();
        for (Bucket bucket: buckets) {

            if (!dyHierarchyDao.isWorking(folders.dyhi)) {
                return;
            }

            String value = null;
            switch (level.getType()) {
                case Term:
                    value = bucket.getKeyAsString();
                    value = value.replace('/', '_');
                    break;
                case Date:
                    value = bucket.getKeyAsString();
                    break;
            }
            folders.push(value, level);
            Aggregations child = bucket.getAggregations();
            if (child.asList().size() > 0) {
                queue.add(new Tuple(child, depth + 1));
                createDynamicHierarchy(queue, folders);
            }
            folders.pop();
        }
    }

    public class FolderStack {

        public final DyHierarchy dyhi;
        private Stack<Folder> stack = new Stack<>();
        public int count = 0;

        public FolderStack(Folder root, DyHierarchy dyhi) {
            this.dyhi = dyhi;
            this.stack.push(root);
        }

        public Folder push(String value, DyHierarchyLevel level) {
            Folder parent = stack.peek();
            Folder folder = folderService.create(new FolderSpec()
                    .setName(value)
                    .setParentId(parent.getId())
                    .setRecursive(false)
                    .setDyhiId(dyhi.getId())
                    .setSearch(getSearch(value, level)), true);
            count++;
            stack.push(folder);
            return folder;
        }

        public void pop() {
            stack.pop();
        }

        public AssetSearch getSearch(String value, DyHierarchyLevel level) {
            AssetSearch search = new AssetSearch();
            switch (level.getType()) {
                case Term:
                    search.getFilter().addToFieldTerms(
                            new AssetFieldTerms(level.getField(), value));
                    break;
                case Date:
                    search.getFilter().setFieldRange(new AssetFieldRange()
                            .setField(level.getField())
                            .setMin(value)
                            .setMax(value)
                            .setFormat((String) level.getOptions().get("format")));

            }
            return search;
        }
    }
}
