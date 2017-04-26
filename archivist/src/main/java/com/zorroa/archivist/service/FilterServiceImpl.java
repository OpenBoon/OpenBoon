package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FilterDao;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 8/9/16.
 */
@Service
@Transactional
public class FilterServiceImpl implements FilterService {

    private static final Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);

    @Autowired
    FilterDao filterDao;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    LogService logService;

    @Autowired
    SearchService searchService;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    Client client;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Override
    public Filter create(FilterSpec spec) {
        // Resolve any names in the ACL.
        spec.setAcl(permissionDao.resolveAcl(spec.getAcl()));
        if (spec.getAcl().isEmpty()) {
            throw new ArchivistWriteException("Cannot have empty ACL when creating a filter");
        }

        Filter filter = filterDao.create(spec);
        createPecolator(filter);

        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create,
                    "filter", filter.getId()));
        });
        return filter;
    }

    @Override
    public Acl getMatchedAcls(Document doc) {
        Map<String, Object> req = ImmutableMap.of("doc", doc.getDocument());
        PercolateResponse response = client.preparePercolate()
                .setIndices(alias)
                .setDocumentType("asset")
                .setSource(Json.prettyString(req))
                .execute().actionGet();

        Acl result = new Acl();
        for(PercolateResponse.Match match : response) {
            int id = Integer.valueOf(match.getId().toString().split("_")[1]);
            result.addAll(filterDao.getAcl(id));
        }
        return result;
    }

    @Override
    public void applyPermissionSchema(Document doc) {
        Acl acl = getMatchedAcls(doc);
        PermissionSchema add = new PermissionSchema();

        /**
         * Convert the ACL to a PermissionSchema.
         */
        for (AclEntry entry: acl) {

            if ((entry.getAccess() & 1) != 0) {
                add.addToRead(entry.getPermissionId());
            }

            if ((entry.getAccess() & 2) != 0) {
                add.addToWrite(entry.getPermissionId());
            }

            if ((entry.getAccess() & 4) != 0) {
                add.addToExport(entry.getPermissionId());
            }
        }

        int userPerm = SecurityUtils.getUser().getPermissionId();
        add.addToRead(userPerm);
        add.addToWrite(userPerm);
        add.addToExport(userPerm);
        doc.setAttr("permissions", add);
    }

    @Override
    public List<Filter> getAll() {
        return filterDao.getAll();
    }

    @Override
    public Filter get(int id) {
        return filterDao.get(id);
    }

    @Override
    public boolean delete(Filter filter) {
        return filterDao.delete(filter.getId());
    }

    @Override
    public boolean setEnabled(Filter filter, boolean value) {
        boolean result = filterDao.setEnabled(filter.getId(), value);
        // If we're disabling
        if (!value) {
            deletePecolator(filter);
        }
        else {
            createPecolator(filter);
        }
        return result;
    }

    @Override
    public PagedList<Filter> getPaged(Pager page) {
        return filterDao.getAll(page);
    }

    private void deletePecolator(Filter filter) {
        DeleteResponse rsp = client.prepareDelete(alias,
                ".percolator", filter.getName()).get("30s");
        if (!rsp.isFound()) {
            logger.warn("Could not find filter percolator: {}", filter.getName());
        }
    }

    /**
     * Create a pecolator for the filter.
     * @param filter
     */
    private void createPecolator(Filter filter) {
        SearchRequestBuilder es = searchService.buildSearch(filter.getSearch());
        client.prepareIndex(alias, ".percolator", filter.getName())
                .setSource(es.toString())
                .setRefresh(true)
                .execute().actionGet("1m");
    }

}
