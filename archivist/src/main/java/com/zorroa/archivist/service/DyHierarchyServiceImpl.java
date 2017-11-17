package com.zorroa.archivist.service;


import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.DyHierarchyDao;
import com.zorroa.archivist.security.SecureRunnable;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.search.AssetScript;
import com.zorroa.sdk.search.AssetSearch;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * DyHierarchyServiceImpl is responsible for the generation of dynamic hierarchies.
 */
@Service
public class DyHierarchyServiceImpl implements DyHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(DyHierarchyServiceImpl.class);

    @Autowired
    DyHierarchyDao dyHierarchyDao;

    @Autowired
    FolderService folderService;

    @Autowired
    EventLogService logService;

    @Autowired
    SearchService searchService;

    @Autowired
    Client client;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    UniqueTaskExecutor folderTaskExecutor;

    @Value("${archivist.dyhi.maxFoldersPerLevel}")
    private Integer maxFoldersPerLevel;

    @Override
    @Transactional
    public boolean update(int id, DyHierarchy spec) {
        DyHierarchy current = dyHierarchyDao.get(id);

        if (!dyHierarchyDao.update(id, spec)) {
            return false;
        }

        DyHierarchy updated = dyHierarchyDao.get(id);

        /*
         * Folder ID change
         */
        Folder folderOld = folderService.get(current.getFolderId());
        Folder folderNew = folderService.get(updated.getFolderId());
        if (folderOld != folderNew ) {
            folderService.removeDyHierarchyRoot(folderOld);
        }

        /*
         * Even if the folder didn't change, we reset so the smart query
         * gets updated.
         */
        String field = updated.getLevel(0).getField();
        folderService.setDyHierarchyRoot(folderNew, field);

        /*
         * If this returns true, the dyhi was working, it should stop
         * pretty quickly.  Other transactions should be blocked at
         * the update() call, but there might be some races in here.
         */
        if (dyHierarchyDao.setWorking(updated, false)) {
            logger.info("DyHi {} was running, will wait on folders to be removed.");
        }

        /*
         * Queue up an event where after this transaction commits.
         */
        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(UserLogSpec.build(LogAction.Update, updated));
            folderService.deleteAll(current);
            submitGenerate(dyHierarchyDao.get(current.getId()));
        });

        return true;
    }

    @Override
    @Transactional
    public boolean delete(DyHierarchy dyhi) {

        if (dyHierarchyDao.delete(dyhi.getId())) {
            Folder folder = folderService.get(dyhi.getFolderId());
            folderService.removeDyHierarchyRoot(folder);

            if (dyHierarchyDao.setWorking(dyhi, false)) {
                logger.info("DyHi {} was running, will wait on folders to be removed.");
            } else {
                folderService.deleteAll(dyhi);
            }
            transactionEventManager.afterCommitSync(() -> {
                logService.logAsync(UserLogSpec.build(LogAction.Delete, dyhi));
            });
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public DyHierarchy create(DyHierarchySpec spec) {

        Folder folder = folderService.get(spec.getFolderId());
        DyHierarchy dyhi = dyHierarchyDao.create(spec);
        folderService.setDyHierarchyRoot(folder, spec.getLevels().get(0).getField());

        if (ArchivistConfiguration.unittest) {
            generate(dyhi);
        }
        else {
            /*
             * Generate the hierarchy in another thread
             * after this method returns.
             */
            transactionEventManager.afterCommitSync(()-> {
                logService.logAsync(UserLogSpec.build(LogAction.Create, dyhi));
                submitGenerate(dyhi);
            });
        }

        return dyhi;
    }

    @Override
    public DyHierarchy get(int id) {
        return dyHierarchyDao.get(id);
    }

    @Override
    public DyHierarchy get(Folder folder) {
        return dyHierarchyDao.get(folder);
    }

    @Override
    public void generateAll() {
        for (DyHierarchy dyhi: dyHierarchyDao.getAll()) {
            generate(dyhi);
        }
    }

    @Override
    public void submitGenerateAll(final boolean refresh) {
        folderTaskExecutor.execute(
                new UniqueRunnable("dyhi_run_all", new SecureRunnable(() -> {
            if (refresh) {
                ElasticClientUtils.refreshIndex(client);
            }
            generateAll();
        }, SecurityContextHolder.getContext())));
    }

    @Override
    public void submitGenerate(DyHierarchy dyhi) {
        folderTaskExecutor.execute(new UniqueRunnable("dyhi_run_"+ dyhi.getId(),
                    new SecureRunnable(() -> generate(dyhi), SecurityContextHolder.getContext())));
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

        Folder rf = folderService.get(dyhi.getFolderId());


        /**
         * TODO: allow some custom search options here, for example, maybe you
         * want to agg for the last 24 hours.
         */
        try {
            SearchRequestBuilder srb = client.prepareSearch()
                    .setQuery(rf.getSearch() == null ?
                            QueryBuilders.matchAllQuery() : searchService.getQuery(rf.getSearch()))
                    .setSize(0);

            /**
             * Fix the field name to take into account raw.
             */
            for (DyHierarchyLevel level: dyhi.getLevels()) {
                resolveFieldName(level);
            }

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

            /**
             * Delete all unmarked folders.
             */
            Set<Integer> unusedFolders = Sets.difference(ImmutableSet.copyOf(folderService.getAllIds(dyhi)), folders.folderIds);
            try {
                folderService.deleteAll(unusedFolders);
            } catch (Exception e) {
                logger.warn("Failed to delete unused folders: {}, {}", unusedFolders, e);
            }

            logger.info("{} created by {}, {} folders", dyhi, SecurityUtils.getUsername(), folders.count);
            return folders.count;
        }
        catch (Exception e) {
            logger.warn("Failed to generate dynamic hierarchy,", e);
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

        return 0;
    }

    private AggregationBuilder elasticAggregationBuilder(DyHierarchyLevel level, int idx) {

        switch(level.getType()) {
            case Attr:
                TermsBuilder terms = AggregationBuilders.terms(String.valueOf(idx));
                terms.field(level.getField());
                terms.size(maxFoldersPerLevel);
                return terms;
            case Year:
                DateHistogramBuilder year = AggregationBuilders.dateHistogram(String.valueOf(idx));
                year.field(level.getField());
                year.interval(new DateHistogramInterval("1y"));
                year.format("yyyy");
                year.minDocCount(1);
                return year;
            case Month:
                DateHistogramBuilder month = AggregationBuilders.dateHistogram(String.valueOf(idx));
                month.field(level.getField());
                month.interval(new DateHistogramInterval("1M"));
                month.format("M");
                month.minDocCount(1);
                return month;
            case Day:
                DateHistogramBuilder day = AggregationBuilders.dateHistogram(String.valueOf(idx));
                day.field(level.getField());
                day.interval(new DateHistogramInterval("1d"));
                day.format("d");
                day.minDocCount(1);
                return day;
            case Path:
                TermsBuilder pathTerms = AggregationBuilders.terms(String.valueOf(idx));
                pathTerms.field(level.getField());
                pathTerms.size(maxFoldersPerLevel);
                return pathTerms;
        }

        return null;
    }

    /**
     * Breadth first walk of aggregations which builds the folder structure as it walks.
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

            boolean popit = true;

            String value;
            String name;
            switch (level.getType()) {
                case Attr:
                    value = bucket.getKeyAsString();
                    name = value.replace('/', '_');
                    folders.push(name, level, value, null);
                    break;
                case Path:
                    value = bucket.getKeyAsString();
                    String delimiter = level.getOptions().getOrDefault("delimiter", "/").toString();

                    Folder peek = folders.peek();
                    folders.stash();
                    popit = false;
                    for (String val: new PathIterator(value, delimiter)) {
                        name = Iterables.getLast(Splitter.on(delimiter).omitEmptyStrings().splitToList(val));
                        folders.push(name, level, val, peek);
                    }
                    break;
                default:
                    value = bucket.getKeyAsString();
                    folders.push(value, level, null, null);
                    break;
            }

            Aggregations child = bucket.getAggregations();
            if (child.asList().size() > 0) {
                queue.add(new Tuple(child, depth + 1));
                createDynamicHierarchy(queue, folders);
            }
            if (popit) {
                folders.pop();
            }
            else {
                folders.unstash();
            }
        }
    }

    public class FolderStack {

        public final DyHierarchy dyhi;
        private Stack<Folder> stack = new Stack<>();
        private Stack<Folder> stash = new Stack<>();
        public int count = 0;
        public final Set<Integer> folderIds = Sets.newHashSetWithExpectedSize(50);
        public final Folder root;

        public FolderStack(Folder root, DyHierarchy dyhi) {
            this.dyhi = dyhi;
            this.stack.push(root);
            this.root = root;
        }

        /**
         * Stash the current stack as the new root.
         */
        public void stash() {
            stash.clear();
            stash.addAll(stack);
        }

        public void unstash() {
            stack.clear();
            stack.addAll(stash);
        }

        public void pop() {
            stack.pop();
        }

        public Folder peek() {
            try {
                return stack.peek();
            }
            catch (NullPointerException e) {
            }
            return null;
        }

        /**
         * Create a folder at a given level and pushes the result
         * onto the stack.
         *
         * @param value
         * @param level
         * @return
         */
        public Folder push(String value, DyHierarchyLevel level, String queryValue, Folder searchParent) {
            Folder parent = stack.peek();
            AssetSearch search = getSearch(queryValue == null ? value : queryValue, level);

            /*
             * The parent search is merged into the current folder's search.
             */
            if (searchParent != null) {
                if (searchParent.getSearch() != null) {
                    search.getFilter().merge(searchParent.getSearch().getFilter());
                }
            }
            else if (parent.getSearch() != null) {
                search.getFilter().merge(parent.getSearch().getFilter());
            }

            String name = getFolderName(value, level);

            /*
             * Create a non-recursive
             */
            FolderSpec spec = new FolderSpec()
                    .setName(name)
                    .setParentId(parent.getId())
                    .setRecursive(false)
                    .setDyhiId(dyhi.getId())
                    .setSearch(search);

            Folder folder = folderService.create(spec, true);
            if (level.getAcl() != null && spec.created) {
                /**
                 * If the ACL is set, build an ACL based on the name.
                 */
                Acl acl = new Acl();
                for (AclEntry entry: level.getAcl()) {
                    String perm = entry.getPermission().replaceAll("%\\{name\\}", name);
                    acl.add(new AclEntry().setPermission(perm).setAccess(entry.getAccess()));
                }
                folderService.setAcl(folder, acl, true, true);
            }

            count++;
            stack.push(folder);
            // Make note of the folder ID.
            folderIds.add(folder.getId());
            return folder;
        }

        /**
         * Take a value and a level and return the proper folder name.  This
         * will eventually support more formatting options.
         *
         * @param value
         * @param level
         * @return
         */
        public String getFolderName(String value, DyHierarchyLevel level) {
            switch(level.getType()) {
                case Attr:
                case Year:
                case Day:
                    return value;
                case Month:
                    return new java.text.DateFormatSymbols().getMonths()[Integer.parseInt(value) - 1];
                default:
                    return value;
            }
        }

        /**
         * Build a search from the given folder name and level.
         *
         * @param value
         * @param level
         * @return
         */
        public AssetSearch getSearch(String value, DyHierarchyLevel level) {
            AssetSearch search = new AssetSearch();
            switch (level.getType()) {
                case Attr:
                    search.addToFilter().addToTerms(level.getField(), Lists.newArrayList(value));
                    break;
                case Year:
                    search.addToFilter().addToScripts(new AssetScript(
                            String.format("doc['%s'].getYear() == value", level.getField()),
                            ImmutableMap.of("value", Integer.valueOf(value))));
                    break;
                case Month:
                    search.addToFilter().addToScripts(new AssetScript(
                            String.format("doc['%s'].getMonth() == value", level.getField()),
                            ImmutableMap.of("value", Integer.valueOf(value) - 1)));
                    break;
                case Day:
                    search.addToFilter().addToScripts(new AssetScript(
                            String.format("doc['%s'].getDayOfMonth() == value", level.getField()),
                            ImmutableMap.of("value", Integer.valueOf(value))));
                    break;
                case Path:
                    // chop off trailing /
                    value = value.substring(0, value.length()-1);
                    search.addToFilter().addToTerms(level.getField(), ImmutableList.of(value));
                    break;
            }
            return search;
        }
    }

    Set<DyHierarchyLevelType> FORCE_RAW_TYPES = EnumSet.of(DyHierarchyLevelType.Attr);

    private void resolveFieldName(DyHierarchyLevel level) {
        if (level.getField().endsWith(".raw") || !FORCE_RAW_TYPES.contains(level.getType())) {
            return;
        }

        String type = ElasticClientUtils.getFieldType(client, "archivist", "asset", level.getField());
        if (!type.equals("string")) {
            return;
        }

        level.setField(level.getField().concat(".raw"));
    }
}
