package com.zorroa.archivist.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.exception.DuplicateElementException;
import com.zorroa.archivist.sdk.service.FolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    public List<Folder> getAll(Collection<String> ids) {
        return folderDao.getAll(ids);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return folderDao.getChildren(folder);
    }

    @Override
    public synchronized Folder create(FolderBuilder builder) {
        try {
            if (folderDao.exists(builder.getParentId(), builder.getName())) {
                throw new DuplicateElementException(String.format("The folder '%s' already exists.", builder.getName()));
            }
            return folderDao.create(builder);
        } finally {
            invalidate(null, builder.getParentId());
        }
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        try {
            return folderDao.update(folder, builder);
        } finally {
            invalidate(folder, builder.getParentId());
        }
    }

    @Override
    public boolean delete(Folder folder) {
        try {
            return folderDao.delete(folder);
        } finally {
            invalidate(folder);
        }
    }

    private void invalidate(Folder folder, String ... additional) {
        if (folder != null) {
            childCache.invalidate(folder.getParentId());
            childCache.invalidate(folder.getId());
        }
        for (String id: additional) {
            childCache.invalidate(id);
        }
    }

    @Override
    public Set<Folder> getAllDescendants(Folder folder) {
        return getAllDescendants(Lists.newArrayList(folder), false);
    }

    @Override
    public Set<Folder> getAllDescendants(Collection<Folder> startFolders, boolean includeStartFolders) {
        Set<Folder> result = Sets.newHashSetWithExpectedSize(25);
        Queue<Folder> queue = Queues.newLinkedBlockingQueue();

        if (includeStartFolders) {
            result.addAll(startFolders);
        }

        queue.addAll(startFolders);
        getChildFoldersRecursive(result, queue);
        return result;
    }

    private final LoadingCache<String, List<Folder>> childCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, List<Folder>>() {
                public List<Folder> load(String key) throws Exception {
                    return folderDao.getChildren(key);
                }
            });

    /**
     * A non-recursion based search for finding all child folders
     * of a folder.
     *
     * @param result
     * @param toQuery
     */
    private void getChildFoldersRecursive(Set<Folder> result, Queue<Folder> toQuery) {

        while(true) {
            Folder current = toQuery.poll();
            if (current == null) {
                return;
            }
            if (Folder.isRoot(current)) {
                continue;
            }

            try {
                List<Folder> children = childCache.get(current.getId());
                if (children == null || children.isEmpty()) {
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
