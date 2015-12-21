package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.domain.Message;
import com.zorroa.archivist.sdk.domain.MessageType;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class FolderServiceImpl implements FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

    @Autowired
    FolderDao folderDao;

    @Autowired
    MessagingService messagingService;

    @Override
    public Folder get(int id) {
        return folderDao.get(id);
    }

    @Override
    public Folder get(int parent, String name) {
        return folderDao.get(parent, name);
    }

    @Override
    public Folder get(String path) {
        logger.info("path: {}", path);
        int parentId = Folder.ROOT_ID;
        Folder current = null;
        for (String name: Splitter.on("/").omitEmptyStrings().trimResults().split(path)) {
            current = folderDao.get(parentId, name);
            parentId = current.getId();
        }
        if (current == null) {
            throw new EmptyResultDataAccessException("Failed to find folder path: " + path, 1);
        }
        return current;
    }


    @Override
    public List<Folder> getAll() {
        return folderDao.getChildren(Folder.ROOT_ID);
    }

    @Override
    public List<Folder> getAll(Collection<Integer> ids) {
        return folderDao.getAll(ids);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return folderDao.getChildren(folder);
    }

    @Override
    public synchronized Folder create(FolderBuilder builder) {
        try {
            Folder folder = folderDao.create(builder);
            messagingService.sendToActiveRoom(new Message(MessageType.FOLDER_CREATE, folder));
            return folder;
        } finally {
            invalidate(null, builder.getParentId());
        }
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        try {
            boolean result = folderDao.update(folder, builder);
            if (result) {
                messagingService.sendToActiveRoom(new Message(MessageType.FOLDER_UPDATE,
                        get(folder.getId())));
            }
            return result;
        } finally {
            invalidate(folder, builder.getParentId());
        }
    }

    @Override
    public boolean delete(Folder folder) {
        try {
            boolean result = folderDao.delete(folder);
            if (result) {
                messagingService.sendToActiveRoom(new Message(MessageType.FOLDER_DELETE, folder));
            }
            return result;
        } finally {
            invalidate(folder);
        }
    }

    private void invalidate(Folder folder, int ... additional) {
        if (folder != null) {
            childCache.invalidate(folder.getParentId());
            childCache.invalidate(folder.getId());
        }
        for (int id: additional) {
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

    private final LoadingCache<Integer, List<Folder>> childCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<Integer, List<Folder>>() {
                public List<Folder> load(Integer key) throws Exception {
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
