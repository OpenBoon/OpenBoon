package com.zorroa.archivist.ingestors;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 1/28/16.
 */
public class PermissionIngestor extends IngestProcessor {

    @Autowired
    UserService userService;

    Set<Permission> export;
    Set<Permission> search;
    Set<Permission> write;

    @Override
    public void init(Ingest ingest) {

        List<String> _export = getArg("export");
        List<String> _search = getArg("search");
        List<String> _write = getArg("write");

        export = getPermissionsByName(_export);
        search = getPermissionsByName(_search);
        write = getPermissionsByName(_write);
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        if (!export.isEmpty()) {
            assetBuilder.setExportPermissions(export);
        }

        if (!write.isEmpty()) {
            assetBuilder.setWritePermissions(write);
        }

        if (!search.isEmpty()) {
            assetBuilder.setSearchPermissions(search);
        }
    }

    private Set<Permission> getPermissionsByName(List<String> names) {
        if (names == null || names.isEmpty()) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder builder = ImmutableSet.builder();
        for (String name: names) {
            try {
                builder.add(userService.getPermission(name));
            } catch (EmptyResultDataAccessException e) {
                logger.warn("Failed to find permission '{}'", name);
            }
        }
        return builder.build();
    }
}
