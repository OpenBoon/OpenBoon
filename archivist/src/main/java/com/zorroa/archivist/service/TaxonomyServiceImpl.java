package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FieldDao;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.repository.TaxonomyDao;
import com.zorroa.archivist.security.InternalAuthentication;
import com.zorroa.archivist.security.InternalRunnable;
import com.zorroa.common.elastic.CountingBulkListener;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by chambers on 6/17/17.
 */
@Service
@Transactional
public class TaxonomyServiceImpl implements TaxonomyService {

    private static final Logger logger = LoggerFactory.getLogger(TaxonomyServiceImpl.class);

    @Autowired
    TaxonomyDao taxonomyDao;

    @Autowired
    FieldDao fieldDao;

    @Autowired
    FolderDao folderDao;

    @Autowired
    FolderService folderService;

    @Autowired
    SearchService searchService;

    @Autowired
    Client client;

    @Autowired
    UniqueTaskExecutor folderTaskExecutor;

    @Autowired
    UserService userService;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    Set<String> EXCLUDE_FOLDERS = ImmutableSet.of("Library", "Users");
    /**
     * Number of entries to write at one time.
     */
    private static final int BULK_SIZE = 100;

    /**
     * Number of assets to pull on each page.
     */
    private static final int PAGE_SIZE = 100;


    private static final String ROOT_FIELD = "zorroa.taxonomy";

    @Override
    public boolean delete(Taxonomy tax, boolean untag) {
        if (taxonomyDao.delete(tax.getTaxonomyId())) {
            // MHide the tax field, appending the . is needed for everything
            // under the field

            Folder folder = folderDao.get(tax.getFolderId());
            folderDao.setTaxonomyRoot(folder, false);
            folderService.invalidate(folder);
            if (untag) {
                untagTaxonomyAsync(tax, 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public Taxonomy create(TaxonomySpec spec) {
        Folder folder = folderService.get(spec.getFolderId());
        List<Folder> ancestors = folderService.getAllAncestors(folder, true, true);
        for (Folder an: ancestors) {
            if (an.isTaxonomyRoot()) {
                throw new ArchivistWriteException("The folder is already in a taxonomy");
            }
        }

        Taxonomy tax =  taxonomyDao.create(spec);

        if (folder.getParentId() == null) {
            throw new ArchivistWriteException("The root folder cannot be a taxonomy.");
        }

        if (EXCLUDE_FOLDERS.contains(folder.getName()) && folder.getParentId() == 0) {
            throw new ArchivistWriteException("This folder cannot hold a taxonomy.");
        }

        boolean result = folderDao.setTaxonomyRoot(folder, true);
        if (result) {
            folderService.invalidate(folder);
            folder.setTaxonomyRoot(true);

            // force is false because there won't be folders with this tax id.
            tagTaxonomyAsync(tax, folder, false);
            return tax;
        }
        else {
            throw new ArchivistWriteException(
                    "Failed to create taxonomy, unable to set taxonomy on folder: " + folder);
        }
    }

    @Override
    public Taxonomy get(int id) {
        return taxonomyDao.get(id);
    }

    @Override
    public Taxonomy get(Folder folder) {
        return taxonomyDao.get(folder);
    }

    @Override
    public void runAllAsync() {
        folderTaskExecutor.execute(
                    new UniqueRunnable("tax_run_all",
                            new InternalRunnable(getAuth(), ()->runAll())));
    }

    @Override
    public void runAll() {
        for (Taxonomy tax:  taxonomyDao.getAll()) {
            tagTaxonomy(tax, null, false);
        }
    }

    @Override
    public void tagTaxonomyAsync(Taxonomy tax, Folder start, boolean force) {
        if (ArchivistConfiguration.unittest) {
            tagTaxonomy(tax, start, force);
        }
        else {
            folderTaskExecutor.execute(new UniqueRunnable("tax_run_" + start.getId(),
                    new InternalRunnable(getAuth(), () -> tagTaxonomy(tax, start, force))));
        }
    }

    @Override
    public Map<String, Long> tagTaxonomy(Taxonomy tax, Folder start, boolean force) {

        logger.info("Tagging taxonomy: {}", tax);

        if (start == null) {
            start = folderService.get(tax.getFolderId());
        }
        long updateTime = System.currentTimeMillis();

        LongAdder folderTotal = new LongAdder();
        LongAdder assetTotal = new LongAdder();

        taxonomyDao.setActive(tax ,true);
        try {
            for (Folder folder : folderService.getAllDescendants(Lists.newArrayList(start), true, false)) {
                folderTotal.increment();

                /**
                 * Walking back is currently the only way to determine the keyword list,
                 * but most calls to are cached.
                 */
                List<Folder> ancestors = folderService.getAllAncestors(folder, true, true);
                List<String> keywords = Lists.newArrayList();

                boolean foundRoot = false;
                if (!folder.isTaxonomyRoot()) {
                    for (Folder f : ancestors) {
                        keywords.add(f.getName());
                        if (f.isTaxonomyRoot()) {
                            foundRoot = true;
                            break;
                        }
                    }
                }
                else {
                    keywords.add(folder.getName());
                    foundRoot = true;
                }

                if (!foundRoot) {
                    logger.warn("Unable to find taxonomy root for folder: {}", folder);
                    break;
                }

                CountingBulkListener cbl = new CountingBulkListener();
                BulkProcessor bulkProcessor = BulkProcessor.builder(
                        client, cbl)
                        .setBulkActions(BULK_SIZE)
                        .setFlushInterval(TimeValue.timeValueSeconds(10))
                        .setConcurrentRequests(0)
                        .build();


                AssetSearch search = folder.getSearch();
                if (search == null) {
                    search = new AssetSearch(new AssetFilter()
                            .addToTerms("links.folder", folder.getId())
                            .setRecursive(false));
                }

                // If it is not a force, then skip over fields already written.
                if (!force) {
                    search.getFilter().setMustNot(ImmutableList.of(
                            new AssetFilter().addToTerms(ROOT_FIELD + ".taxId", tax.getTaxonomyId())));
                }

                Stopwatch timer = Stopwatch.createStarted();
                SearchResponse rsp = searchService.buildSearch(search, "asset")
                        .setScroll(new TimeValue(60000))
                        .setFetchSource(true)
                        .setSize(PAGE_SIZE).execute().actionGet();
                logger.info("tagging taxonomy {} batch 1 : {}", tax, timer);

                TaxonomySchema taxy = new TaxonomySchema()
                        .setFolderId(folder.getId())
                        .setTaxId(tax.getTaxonomyId())
                        .setUpdatedTime(updateTime)
                        .setKeywords(keywords)
                        .setSuggest(keywords);

                int batchCounter = 1;
                try {
                    do {
                        for (SearchHit hit : rsp.getHits().getHits()) {

                            Document doc = new Document(hit.getSource());
                            Set<TaxonomySchema> taxies = doc.getAttr(ROOT_FIELD, new TypeReference<Set<TaxonomySchema>>() {});
                            if (taxies == null) {
                                taxies = Sets.newHashSet();
                            }
                            taxies.add(taxy);
                            doc.setAttr(ROOT_FIELD, taxies);
                            bulkProcessor.add(client.prepareIndex(alias, "asset", hit.getId())
                                    .setOpType(IndexRequest.OpType.INDEX)
                                    .setSource(Json.serialize(doc.getDocument())).request());
                        }

                        rsp = client.prepareSearchScroll(rsp.getScrollId()).setScroll(
                                new TimeValue(60000)).execute().actionGet();
                        batchCounter++;
                        logger.info("tagging {} batch {} : {}", tax, batchCounter, timer);


                    } while (rsp.getHits().getHits().length != 0);
                } catch (Exception e) {
                    logger.warn("Failed to tag taxonomy assets: {}", tax);
                } finally {
                    bulkProcessor.close();
                }
                assetTotal.add(cbl.getSuccessCount());
            }

            if (force) {
                untagTaxonomyAsync(tax, updateTime);
            }
        }
        finally {
            taxonomyDao.setActive(tax, false);
        }


        logger.info("Taxonomy {} executed, {} assets updated in {} folders",
                tax.getFolderId(), assetTotal.longValue(), folderTotal.intValue());

        if (assetTotal.longValue() > 0) {
            searchService.invalidateFields();
        }

        return ImmutableMap.of(
                "assetCount", assetTotal.longValue(),
                "folderCount", folderTotal.longValue(),
                "timestamp", updateTime);
    }

    @Override
    public void untagTaxonomyAsync(Taxonomy tax, long timestamp) {
        folderTaskExecutor.execute(new InternalRunnable(getAuth(), () -> untagTaxonomy(tax, timestamp)));
    }

    @Override
    public void untagTaxonomyAsync(Taxonomy tax) {
        folderTaskExecutor.execute(new InternalRunnable(getAuth(), () -> untagTaxonomy(tax)));
    }

    @Override
    public void untagTaxonomyFoldersAsync(Taxonomy tax, List<Folder> folders) {
        folderTaskExecutor.execute(new InternalRunnable(getAuth(), () -> untagTaxonomyFolders(tax, folders)));
    }

    @Override
    public void untagTaxonomyFoldersAsync(Taxonomy tax, Folder folder, List<String> assets) {
        folderTaskExecutor.execute(new InternalRunnable(getAuth(), () -> untagTaxonomyFolders(tax, folder, assets)));
    }

    /**
     * Untag specific assets in a given folder.  This is run when asstets
     * are removed from a folder.
     *
     * @param tax
     * @param folder
     * @param assets
     */
    @Override
    public void untagTaxonomyFolders(Taxonomy tax, Folder folder, List<String> assets) {
        logger.warn("Untagging {} on {} assets {}", tax, folder, assets);

        CountingBulkListener cbl = new CountingBulkListener();
        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build();

        AssetSearch search = new AssetSearch();
        search.setFilter(new AssetFilter()
                .addToTerms("_id", assets)
                .addToTerms("zorroa.taxonomy.folderId", folder.getId()));

        SearchResponse rsp = client.prepareSearch("archivist")
                .setScroll(new TimeValue(60000))
                .setFetchSource(true)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet();

        processBulk(bulkProcessor, rsp, ts -> ts.getTaxId() == tax.getTaxonomyId());

        bulkProcessor.close();
        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

    }

    /**
     * Untag specific taxonomy folders folders.  This happens when
     * the folders are deleted.
     *
     * @param tax
     * @param folders
     */
    @Override
    public void untagTaxonomyFolders(Taxonomy tax,  List<Folder> folders) {
        logger.warn("Untagging {} on {} folders", tax, folders.size());

        List<Integer> folderIds = folders.stream().map(
                f->f.getId()).collect(Collectors.toList());

        ElasticClientUtils.refreshIndex(client, 1);
        for (List<Integer> list: Lists.partition(folderIds, 500)) {

            CountingBulkListener cbl = new CountingBulkListener();
            BulkProcessor bulkProcessor = BulkProcessor.builder(
                    client, cbl)
                    .setBulkActions(BULK_SIZE)
                    .setFlushInterval(TimeValue.timeValueSeconds(10))
                    .setConcurrentRequests(0)
                    .build();

            AssetSearch search = new AssetSearch();
            search.setFilter(new AssetFilter()
                    .addToTerms("zorroa.taxonomy.folderId", list));

            SearchResponse rsp = client.prepareSearch("archivist")
                    .setScroll(new TimeValue(60000))
                    .setFetchSource(false)
                    .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                    .setQuery(searchService.getQuery(search))
                    .setSize(PAGE_SIZE).execute().actionGet();

            processBulk(bulkProcessor, rsp, ts -> ts.getTaxId() == tax.getTaxonomyId());

            bulkProcessor.close();
            logger.info("Untagged: {} success:{} errors: {}", tax,
                    cbl.getSuccessCount(), cbl.getErrorCount());
        }
    }

    /**
     * Called to untag an entire taxonomy if the taxonomy is deleted.
     *
     * @param tax
     * @return
     */
    @Override
    public Map<String, Long> untagTaxonomy(Taxonomy tax) {
        logger.info("Untagging entire taxonomy {}", tax);
        CountingBulkListener cbl = new CountingBulkListener();
        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build();

        AssetSearch search = new AssetSearch();
        search.setFilter(new AssetFilter()
                .addToTerms("zorroa.taxonomy.taxId", tax.getTaxonomyId()));

        SearchResponse rsp = client.prepareSearch("archivist")
                .setScroll(new TimeValue(60000))
                .setFetchSource(true)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet();

        processBulk(bulkProcessor, rsp, ts -> ts.getTaxId() == tax.getTaxonomyId());

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount());
    }

    /**
     * Untags assets which were part of a taxonomy but are no longer.  Pass
     * in a 0 for timestamp to mean all assets get untagged.
     *
     * @param tax
     * @param timestamp
     * @return
     */
    @Override
    public Map<String, Long> untagTaxonomy(Taxonomy tax, long timestamp) {

        logger.info("Untagging assets no longer tagged tagged: {} {}", tax, timestamp);
        ElasticClientUtils.refreshIndex(client, 1);

        CountingBulkListener cbl = new CountingBulkListener();
        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build();

        /**
         * This filters out assets with a new timestamp.
         */
        AssetSearch search = new AssetSearch();
        search.setFilter(new AssetFilter().addToTerms("zorroa.taxonomy.taxId", tax.getTaxonomyId()));

        SearchResponse rsp = client.prepareSearch("archivist")
                .setScroll(new TimeValue(60000))
                .setFetchSource(true)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet();

        processBulk(bulkProcessor, rsp,
                ts -> (ts.getTaxId() == tax.getTaxonomyId() && ts.getUpdatedTime() != timestamp));

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount(),
                "timestamp", timestamp);
    }

    private void processBulk(BulkProcessor bulkProcessor, SearchResponse rsp, Predicate<TaxonomySchema> pred) {
        try {
            do {
                for (SearchHit hit : rsp.getHits().getHits()) {
                    Document doc = new Document(hit.getSource());
                    Set<TaxonomySchema> taxies = doc.getAttr(ROOT_FIELD, new TypeReference<Set<TaxonomySchema>>() {});
                    if (taxies!= null) {
                        if (taxies.removeIf(pred)) {
                            doc.setAttr(ROOT_FIELD, taxies);
                            bulkProcessor.add(client.prepareIndex("archivist", "asset", hit.getId())
                                    .setOpType(IndexRequest.OpType.INDEX)
                                    .setSource(Json.serialize(doc.getDocument())).request());
                        }
                    }
                }
                rsp = client.prepareSearchScroll(rsp.getScrollId()).setScroll(
                        new TimeValue(60000)).execute().actionGet();

            } while (rsp.getHits().getHits().length != 0);

        } catch (Exception e) {
            logger.warn("Failed to untag taxonomy assets, ", e);

        }
        finally {
            bulkProcessor.close();
        }
    }

    private Authentication getAuth() {
        User user = userService.get("admin");
        return  new InternalAuthentication(user, userService.getPermissions(user));
    }
}
