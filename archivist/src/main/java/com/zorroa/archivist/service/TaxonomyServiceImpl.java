package com.zorroa.archivist.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FieldDao;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.repository.TaxonomyDao;
import com.zorroa.common.elastic.CountingBulkListener;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
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

    @Override
    public boolean delete(Taxonomy tax, boolean untag) {
        if (taxonomyDao.delete(tax.getTaxonomyId())) {
            // MHide the tax field, appending the . is needed for everything
            // under the field
            searchService.updateField(
                    new HideField(tax.getRootField() + ".", true));

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
        if (ArchivistConfiguration.unittest) {
            runAll();
        }
        else {
            folderTaskExecutor.execute(
                    new UniqueRunnable("tax_run_all", () -> runAll()));
        }
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
                    () -> tagTaxonomy(tax, start, force)));
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

        String rootField = String.format("zorroa.taxonomy.tax%d", tax.getTaxonomyId());

        /**
         * TODO: Going to change this to be more efficient by keeping track of the stack.
         */
        for (Folder folder : folderService.getAllDescendants(Lists.newArrayList(start), true, false)) {
            folderTotal.increment();

            List<Folder> ancestors = folderService.getAllAncestors(folder, true, true);
            List<String> keywords = Lists.newArrayList();

            boolean foundRoot = false;
            for (Folder f: ancestors) {
                keywords.add(f.getName());
                if (f.isTaxonomyRoot()) {
                    foundRoot = true;
                    break;
                }
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
                        new AssetFilter().addToExists(rootField)));
            }

            Stopwatch timer = Stopwatch.createStarted();
            SearchResponse rsp = searchService.buildSearch(search)
                    .setScroll(new TimeValue(60000))
                    .setFetchSource(false)
                    .setSize(PAGE_SIZE).execute().actionGet();
            logger.info("tagging taxonomy {} batch 1 : {}", tax, timer);

            Document doc = new Document();
            doc.setAttr(rootField,
                    ImmutableMap.of(
                            "keywords", keywords,
                            "suggest", keywords,
                            "timestamp", updateTime,
                            "folderId",folder.getId(),
                            "taxId", tax.getTaxonomyId()));

            int batchCounter = 1;
            try {
                do {
                    for (SearchHit hit : rsp.getHits().getHits()) {
                        bulkProcessor.add(client.prepareUpdate(alias, "asset", hit.getId())
                                .setDoc(doc.getDocument()).request());
                    }
                    rsp = client.prepareSearchScroll(rsp.getScrollId()).setScroll(
                            new TimeValue(60000)).execute().actionGet();
                    batchCounter++;
                    logger.info("tagging {} batch {} : {}", tax, batchCounter, timer);


                } while (rsp.getHits().getHits().length != 0);
            } catch (Exception e) {
                logger.warn("Failed to tag taxonomy assets: {}", tax);
            }
            finally {
                bulkProcessor.close();
            }
            assetTotal.add(cbl.getSuccessCount());
        }

        if (force) {
            untagTaxonomyAsync(tax, updateTime);
        }

        logger.info("Taxonomy {} executed, {} assets updated in {} folders",
                tax.getFolderId(), assetTotal.longValue(), folderTotal.intValue());

        return ImmutableMap.of(
                "assetCount", assetTotal.longValue(),
                "folderCount", folderTotal.longValue(),
                "timestamp", updateTime);
    }

    @Override
    public void untagTaxonomyAsync(Taxonomy tax, long timestamp) {
        folderTaskExecutor.execute(() -> untagTaxonomy(tax, timestamp));
    }

    @Override
    public void untagTaxonomyAsync(Taxonomy tax) {
        folderTaskExecutor.execute(() -> untagTaxonomy(tax));
    }

    @Override
    public void untagTaxonomyFoldersAsync(Taxonomy tax, List<Folder> folders) {
        folderTaskExecutor.execute(() -> untagTaxonomyFolders(tax, folders));
    }

    @Override
    public void untagTaxonomyFoldersAsync(Taxonomy tax, Folder folder, List<String> assets) {
        folderTaskExecutor.execute(() -> untagTaxonomyFolders(tax, folder, assets));
    }

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

        String name = "tax" + tax.getTaxonomyId();
        String field = "zorroa.taxonomy." + name;

        AssetSearch search = new AssetSearch();
        search.setFilter(new AssetFilter()
                .addToExists(field)
                .addToTerms("_id", assets)
                .addToTerms(field + ".folderId", folder.getId()));

        SearchResponse rsp = client.prepareSearch("archivist")
                .setScroll(new TimeValue(60000))
                .setFetchSource(false)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet();

        Script script = new Script("ctx._source.zorroa.taxonomy.remove(name)",
                ScriptService.ScriptType.INLINE, "groovy", ImmutableMap.of("name", name));

        processBulk(bulkProcessor, rsp, script);

        bulkProcessor.close();
        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

    }

    @Override
    public void untagTaxonomyFolders(Taxonomy tax,  List<Folder> folders) {
        logger.warn("Untaggng {} on {} folders", tax, folders.size());

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

            String name = "tax" + tax.getTaxonomyId();
            String field = "zorroa.taxonomy." + name;

            AssetSearch search = new AssetSearch();
            search.setFilter(new AssetFilter()
                    .addToExists(field)
                    .addToTerms(field + ".folderId", list));

            SearchResponse rsp = client.prepareSearch("archivist")
                    .setScroll(new TimeValue(60000))
                    .setFetchSource(false)
                    .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                    .setQuery(searchService.getQuery(search))
                    .setSize(PAGE_SIZE).execute().actionGet();

            Script script = new Script("ctx._source.zorroa.taxonomy.remove(name)",
                    ScriptService.ScriptType.INLINE, "groovy", ImmutableMap.of("name", name));

            processBulk(bulkProcessor, rsp, script);

            bulkProcessor.close();
            logger.info("Untagged: {} success:{} errors: {}", tax,
                    cbl.getSuccessCount(), cbl.getErrorCount());
        }
    }

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

        String name = String.format("tax%d", tax.getTaxonomyId());
        String field = String.format("zorroa.taxonomy.%s.taxId", name, tax.getTaxonomyId());

        AssetSearch search = new AssetSearch();
        search.setFilter(new AssetFilter()
                .setMustNot(ImmutableList.of(new AssetFilter().addToTerms(field, tax.getTaxonomyId()))));

        SearchResponse rsp = client.prepareSearch("archivist")
                .setScroll(new TimeValue(60000))
                .setFetchSource(false)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet();

        Script script = new Script("ctx._source.zorroa.taxonomy.remove(name)",
                ScriptService.ScriptType.INLINE, "groovy", ImmutableMap.of("name", name));

        processBulk(bulkProcessor, rsp, script);

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount());
    }

    @Override
    public Map<String, Long> untagTaxonomy(Taxonomy tax, long timestamp) {
        logger.info("Untagging assets not tagged: {}", tax);
        ElasticClientUtils.refreshIndex(client, 1);

        CountingBulkListener cbl = new CountingBulkListener();
        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build();

        String name = String.format("tax%d", tax.getTaxonomyId());
        String field = String.format("zorroa.taxonomy.tax%d.timestamp", tax.getTaxonomyId());

        AssetSearch search = new AssetSearch();
        search.setFilter(new AssetFilter()
                .addToExists(field)
                .setMustNot(ImmutableList.of(new AssetFilter().addToTerms(field, timestamp))));

        SearchResponse rsp = client.prepareSearch("archivist")
                .setScroll(new TimeValue(60000))
                .setFetchSource(false)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet();

        Script script = new Script("ctx._source.zorroa.taxonomy.remove(name)",
                ScriptService.ScriptType.INLINE, "groovy", ImmutableMap.of("name", name));

        processBulk(bulkProcessor, rsp, script);

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount(),
                "timestamp", timestamp);
    }


    private void processBulk(BulkProcessor bulkProcessor, SearchResponse rsp, Script script) {
        try {
            do {
                for (SearchHit hit : rsp.getHits().getHits()) {
                    bulkProcessor.add(client.prepareUpdate("archivist", "asset", hit.getId())
                            .setScript(script).request());
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
}
