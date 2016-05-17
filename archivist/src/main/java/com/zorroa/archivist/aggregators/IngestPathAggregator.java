package com.zorroa.archivist.aggregators;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Aggregator;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.IngestService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.archivist.service.UserService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class IngestPathAggregator extends Aggregator {

    @Autowired
    FolderService folderService;

    @Autowired
    IngestService ingestService;

    @Autowired
    SearchService searchService;

    @Autowired
    UserService userService;

    private Folder ingestFolder;
    private Map<String, Folder> pathMap = Maps.newHashMap();
    private Acl acl;
    private AssetAggregateBuilder pathAggBuilder = null;

    @Override
    public void init(Ingest ingest) {
        super.init(ingest);
        acl = new Acl()
                .addEntry(userService.getPermission("group::superuser"), Access.Write, Access.Read)
                .addEntry(userService.getPermission("group::user"), Access.Read);

        /*
         * TODO: Not sure how to aggregate if the source is a URI and not
         * a file path.  It might involve aggregating by a different field.
         */
        List<String> excludedPaths = Lists.newArrayList();
        for (String path: ingest.getUris()) {

            if (path.startsWith("file:")) {
                path = URI.create(path).getPath();
            }

            if (!FileUtils.isURI(path)) {
                excludedPaths.addAll(FileUtils.superSplit(path));
            }
        }

        if (!excludedPaths.isEmpty()) {
            ingestFolder = ingestService.getFolder(ingest);
            pathAggBuilder = new AssetAggregateBuilder().setName("path")
                    .setField("source.directory.dir").setSearch(ingestFolder.getSearch())
                    .setExclude(String.join("|", excludedPaths));
        }
    }

    @Override
    public void aggregate() {

        if (pathAggBuilder == null) {
            return;
        }

        // Aggregate over the pathIndexed source.directory.dir field to get each path component
        SearchResponse pathReponse = searchService.aggregate(pathAggBuilder);
        Terms pathTerms = pathReponse.getAggregations().get("path");
        Collection<Terms.Bucket> pathBuckets = pathTerms.getBuckets();
        for (Terms.Bucket pathBucket: pathBuckets) {

            String folderPath = (String) pathBucket.getKey();

            /*
             * If the path is null, that means its before the aggregation path.
             */
            if (folderPath == null) {
                continue;
            }
            if (pathMap.containsKey(folderPath)) {
                continue;
            }

            String[] segments = folderPath.split("/");
            String title = segments[segments.length - 1].trim();
            if (title.charAt(0) == '.') {
                continue;
            }

            // Get the parent folder, assumes folders are processed from parents to children!
            String parentPath = folderPath.substring(0, folderPath.lastIndexOf("/"));
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
                        .setField("source.directory.dir").setTerm(folderPath));
                FolderBuilder pathBuilder = new FolderBuilder().setName(title)
                        .setParentId(parentFolder.getId())
                        .setSearch(new AssetSearch().setFilter(pathFilter))
                        .setAcl(acl);
                pathFolder = folderService.create(pathBuilder);
            }
            pathMap.put(folderPath, pathFolder);
        }
    }

}
