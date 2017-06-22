package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
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
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

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
    FolderDao folderDao;

    @Autowired
    FolderService folderService;

    @Autowired
    SearchService searchService;

    @Autowired
    Client client;

    /**
     * Only a single thread can be generating hierarchies currently.
     */
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    Set<String> EXCLUDE_FOLDERS = ImmutableSet.of("Library", "Users");

    @Override
    public boolean delete(Taxonomy tax) {
        if (taxonomyDao.delete(tax.getTaxonomyId())) {
            untagTaxonomyAsync(tax, 0);
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

        boolean result = folderDao.setTaxonomyRoot(folder, tax);
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
            executor.schedule(() -> runAll(), 5, TimeUnit.SECONDS);
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
            executor.schedule(() -> tagTaxonomy(tax, start, force), 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public Map<String, Long> tagTaxonomy(Taxonomy tax, Folder start, boolean force) {

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
                    .setBulkActions(1000)
                    .setBulkSize(new ByteSizeValue(50, ByteSizeUnit.MB))
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

            SearchResponse rsp = client.prepareSearch("archivist")
                    .setScroll(new TimeValue(60000))
                    .setFetchSource(false)
                    .addSort("_doc", SortOrder.ASC)
                    .setQuery(searchService.getQuery(search))
                    .setSize(100).execute().actionGet();


            Document doc = new Document();
            doc.setAttr(rootField,
                    ImmutableMap.of(
                            "keywords", keywords,
                            "timestamp", updateTime,
                            "folderId",folder.getId()));

            while (true) {
                for (SearchHit hit : rsp.getHits().getHits()) {
                    bulkProcessor.add(client.prepareUpdate("archivist", "asset", hit.getId())
                            .setDoc(doc.getDocument()).request());
                }

                rsp = client.prepareSearchScroll(rsp.getScrollId()).setScroll(
                        new TimeValue(60000)).execute().actionGet();
                if (rsp.getHits().getHits().length == 0) {
                    break;
                }
            }
            try {
                bulkProcessor.awaitClose(60, TimeUnit.MINUTES);
            } catch (InterruptedException e) {

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
        if (ArchivistConfiguration.unittest) {
            untagTaxonomy(tax, timestamp);
        }
        else {
            executor.schedule(() ->  untagTaxonomy(tax, timestamp), 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public Map<String, Long> untagTaxonomy(Taxonomy tax, long timestamp) {
        logger.info("Untagging assets not tagged: {}", tax);
        ElasticClientUtils.refreshIndex(client, 1);

        CountingBulkListener cbl = new CountingBulkListener();
        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(1000)
                .setBulkSize(new ByteSizeValue(50, ByteSizeUnit.MB))
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
                .addSort("_doc", SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(100).execute().actionGet();

        Script script = new Script("ctx._source.zorroa.taxonomy.remove(name)",
                ScriptService.ScriptType.INLINE, "groovy", ImmutableMap.of("name", name));

        while (true) {
            for (SearchHit hit : rsp.getHits().getHits()) {
                bulkProcessor.add(client.prepareUpdate("archivist", "asset", hit.getId())
                        .setScript(script).request());
            }

            rsp = client.prepareSearchScroll(rsp.getScrollId()).setScroll(
                    new TimeValue(60000)).execute().actionGet();

            if (rsp.getHits().getHits().length == 0) {
                break;
            }
        }

        bulkProcessor.close();
        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount());

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount(),
                "timestamp", timestamp);
    }
}
