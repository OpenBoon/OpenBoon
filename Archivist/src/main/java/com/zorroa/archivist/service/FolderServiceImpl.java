package com.zorroa.archivist.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.sdk.domain.DuplicateElementException;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.service.FolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class FolderServiceImpl implements FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

    @Autowired
    FolderDao folderDao;

    @Override
    public Folder get(String id) {
        return folderDao.get(id);
    }

    @Override
    public List<Folder> getAll() {
        return folderDao.getChildren(Folder.ROOT_ID);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return folderDao.getChildren(folder);
    }

    @Override
    public List<Folder> getAllDecendents(Folder folder) {
        List<Folder> children = getChildren(folder);
        if (children.isEmpty()) {
            return Lists.newArrayListWithCapacity(0);
        }

        List<Folder> decendents = Lists.newArrayList();
        decendents.addAll(children);

        for (Folder child : children) {
            List<Folder> grandchildren = getAllDecendents(child);
            decendents.addAll(grandchildren);
        }
        return decendents;
    }

    @Override
    public synchronized Folder create(FolderBuilder builder) {
        if (folderDao.exists(builder.getParentId(), builder.getName())) {
            throw new DuplicateElementException(String.format("The folder '%s' already exists.", builder.getName()));
        }
        childCache.invalidate(builder.getParentId());
        return folderDao.create(builder);
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        if (!folder.getParentId().equals(builder.getParentId())) {
            childCache.invalidate(builder.getParentId());
        }
        try {
            return folderDao.update(folder, builder);
        } finally {

        }
    }

    @Override
    public boolean delete(Folder folder) {
        try {
            return folderDao.delete(folder);
        } finally {
            childCache.invalidate(folder.getParentId());
        }
    }

    @Override
    public Set<String> getAllDecendentIds(List<String> folderIds) {
        Set<String> result = Sets.newHashSetWithExpectedSize(100);
        Queue<String> queue = Queues.newLinkedBlockingQueue();

        result.addAll(folderIds);
        queue.addAll(folderIds);
        getChildrenRecursive(result, queue);
        return result;
    }

    private final LoadingCache<String, Set<String>> childCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, Set<String>>() {
                public Set<String> load(String key) throws Exception {
                    Set<String> result =  Collections.synchronizedSet(folderDao.getChildren(key).stream().map(
                            Folder::getId).collect(Collectors.toSet()));
                    return result;
                }
            });

    /**
     * A non-recursion based search for finding all child folders
     * of a folder.
     *
     * @param result
     * @param toQuery
     */
    private void getChildrenRecursive(Set<String> result, Queue<String> toQuery) {

        while(true) {
            String current = toQuery.poll();
            if (current == null) {
                return;
            }
            if (Folder.isRoot(current)) {
                continue;
            }

            try {
                Set<String> children = childCache.get(current);
                if (children.isEmpty()) {
                    continue;
                }
                toQuery.addAll(children);
                result.addAll(children);

            } catch (Exception e) {
                logger.warn("Failed to obtain child folders for {}", current, e);
            }
        }
    }
}
