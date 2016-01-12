package com.zorroa.archivist.ingestors;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.service.SearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Collection;
import java.util.Map;

public class IngestPathAggregator extends IngestProcessor {

    private Folder ingestFolder;
    private String excludePathRoot;
    private Map<String, Folder> pathMap = Maps.newHashMap();

    @Autowired
    FolderService folderService;

    @Autowired
    IngestService ingestService;

    @Autowired
    SearchService searchService;

    @Override
    public void init(Ingest ingest) {
        ingestFolder = ingestService.getFolder(ingest);
        excludePathRoot = ".{1," + Integer.toString(ingest.getPath().length()) + "}";
    }

    @Override
    public void process(AssetBuilder asset) {
        // Aggregate over the pathIndexed source.directory.dir field to get each path component
        AssetAggregateBuilder pathAggBuilder = new AssetAggregateBuilder().setName("path")
                .setField("source.directory.dir").setSearch(ingestFolder.getSearch())
                .setExclude(excludePathRoot);
        SearchResponse pathReponse = searchService.aggregate(pathAggBuilder);
        Terms pathTerms = pathReponse.getAggregations().get("path");
        Collection<Terms.Bucket> pathBuckets = pathTerms.getBuckets();
        for (Terms.Bucket pathBucket: pathBuckets) {

            // Extract the last path component for the title
            String path = pathBucket.getKey();
            String[] segments = path.split("/");
            String title = segments[segments.length - 1].trim();
            if (title.charAt(0) == '.') {
                continue;
            }
            if (pathMap.get(path) != null) {
                continue;
            }

            // Get the parent folder, assumes folders are processed from parents to children!
            String parentPath = path.substring(0, path.lastIndexOf("/"));
            Folder parentFolder = pathMap.get(parentPath);
            if (parentFolder == null) {
                parentFolder = ingestFolder;
            }

            // Create a new folder for this path component
            Folder pathFolder;
            try {
                pathFolder = folderService.get(parentFolder.getId(), title);
            } catch (EmptyResultDataAccessException e) {
                AssetFilter pathFilter = new AssetFilter().setFieldTerm(new AssetFieldTerms()
                        .setField("source.directory.dir").setTerm(path));
                FolderBuilder pathBuilder = new FolderBuilder().setName(title)
                        .setParentId(parentFolder.getId())
                        .setSearch(new AssetSearch().setFilter(pathFilter));
                pathFolder = folderService.create(pathBuilder);
            }
            pathMap.put(path, pathFolder);
        }
    }

}
