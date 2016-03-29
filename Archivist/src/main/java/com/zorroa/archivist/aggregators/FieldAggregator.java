package com.zorroa.archivist.aggregators;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.Aggregator;
import com.zorroa.archivist.sdk.processor.Argument;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.archivist.service.UserService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by wex on 3/5/16.
 */
public class FieldAggregator extends Aggregator {

    @Autowired
    SearchService searchService;

    @Autowired
    FolderService folderService;

    @Autowired
    UserService userService;

    @Argument
    private String name;

    @Argument
    private List<String> fields;

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    private Acl acl;

    @Override
    public void init(Ingest ingest) {
        super.init(ingest);
        acl = new Acl()
                .addEntry(userService.getPermission("internal::server"), Access.Write, Access.Read)
                .addEntry(userService.getPermission("group::user"), Access.Read);
    }

    @Override
    public void aggregate() {
        aggregateField(0, Lists.newArrayList(), null);
    }

    /**
     * Recursive function that builds an aggregation hierarchy on fields
     * @param idx
     * @param parentFolder
     */
    private void aggregateField(int idx, List<Object> parentTerms, Folder parentFolder) {
        if (idx >= fields.size()) {
            return;
        }

        String field = fields.get(idx);
        AssetAggregateBuilder fieldAggBuilder = new AssetAggregateBuilder()
                .setName(field)
                .setField(field)
                .setSearch(parentSearch(parentTerms));
        SearchResponse fieldResponse = searchService.aggregate(fieldAggBuilder);
        Terms fieldTerms = fieldResponse.getAggregations().get(field);
        Collection<Terms.Bucket> fieldBuckets = fieldTerms.getBuckets();

        if (fieldBuckets.size() == 0) {
            return;
        }

        if (parentFolder == null) {
            try {
                parentFolder = folderService.get(0, fields.get(0));
            } catch (EmptyResultDataAccessException e) {
                if (name == null) {
                    name = fields.get(0).replaceAll(".raw", "");
                    int lastDotIdx = name.lastIndexOf(".");
                    if (lastDotIdx > 0) {
                        name = name.substring(lastDotIdx + 1);
                    }
                }
                parentFolder = folderService.create(new FolderBuilder()
                        .setAcl(acl)
                        .setName(name));
            }
        }

        // Create each folder and aggregate over the next level
        for (Terms.Bucket fieldBucket: fieldBuckets) {
            String name = fieldBucket.getKey();
            List<Object> childTerms = new ArrayList<Object>(parentTerms);
            childTerms.add(name);
            Folder fieldFolder = null;
            try {
                fieldFolder = folderService.get(parentFolder.getId(), name);
            } catch (EmptyResultDataAccessException e) {
                fieldFolder = folderService.create(new FolderBuilder()
                        .setName(name)
                        .setParentId(parentFolder.getId())
                        .setSearch(parentSearch(childTerms))
                        .setAcl(acl));
            }

            aggregateField(idx + 1, childTerms, fieldFolder);
        }
    }

    private AssetSearch parentSearch(List<Object> parentTerms) {
        List<AssetFieldTerms> parentFieldTerms = Lists.newArrayList();
        for (int i = 0; i < parentTerms.size(); ++i) {
            parentFieldTerms.add(new AssetFieldTerms().setField(fields.get(i)).setTerm(parentTerms.get(i)));
        }
        return new AssetSearch().setFilter(new AssetFilter().setFieldTerms(parentFieldTerms));
    }
}
