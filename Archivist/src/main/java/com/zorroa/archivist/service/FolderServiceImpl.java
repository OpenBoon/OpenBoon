package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.MessagingService;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class FolderServiceImpl implements FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

    @Autowired
    FolderDao folderDao;

    @Autowired
    AssetDao assetDao;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    MessagingService messagingService;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    DataSourceTransactionManager transactionManager;

    @Override
    public void setAcl(Folder folder, Acl acl) {
        folderDao.setAcl(folder, acl);
        transactionEventManager.afterCommit(()->invalidate(folder));
    }

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
        int parentId = Folder.ROOT_ID;
        Folder current = null;
        for (String name: Splitter.on("/").omitEmptyStrings().trimResults().split(path)) {
            current = folderDao.get(parentId, name);
            parentId = current.getId();
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
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public synchronized Folder create(FolderBuilder builder) {

        Folder parent = folderDao.get(builder.getParentId());
        if (!parent.getAcl().hasAccess(SecurityUtils.getPermissionIds(), Access.Write)) {
            throw new AccessDeniedException("You cannot make changes to this folder");
        }

        /*
          * The current transaction (if any) is suspended by calling the FolderService.create() function.
          * Using transaction template, we create a new transaction, add the folder, and commit it.
         */
        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        Folder result = tmpl.execute(transactionStatus -> {
            try {
                Folder folder = folderDao.create(builder);
                folderDao.setAcl(folder, builder.getAcl());
                transactionEventManager.afterCommitSync(() -> {
                    invalidate(parent);
                    messagingService.broadcast(new Message(MessageType.FOLDER_CREATE, folderDao.get(folder.getId())));
                });
                return folder;
            } catch (DataIntegrityViolationException e) {
               return null;
            }
        });

        /*
          * If null is returned from our new transaction then there was an error creating
          * the folder.  If we expected the folder to be created, an exception is thrown.
          * Otherwise, we return the existing folder.
         */
        if (result == null) {
            if (builder.isExpectCreate()) {
                throw new DataIntegrityViolationException("Folder '" + builder.getName() + "' already exists");
            }
            else {
                result = folderDao.get(builder.getParentId(), builder.getName());
            }
        }

        return result;
    }

    @Override
    public boolean update(Folder folder, FolderUpdateBuilder builder) {

        if(!folder.getAcl().hasAccess(SecurityUtils.getPermissionIds(), Access.Write)) {
            throw new AccessDeniedException("You cannot make changes to this folder");
        }

        boolean result = folderDao.update(folder, builder);
        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                invalidate(folder, folder.getParentId());
                messagingService.broadcast(new Message(MessageType.FOLDER_UPDATE,
                        get(folder.getId())));
            });
        }
        return result;
    }

    @Override
    public boolean delete(Folder folder) {

        if(!folder.getAcl().hasAccess(SecurityUtils.getPermissionIds(), Access.Write)) {
            throw new AccessDeniedException("You cannot make changes to this folder");
        }

        boolean result = folderDao.delete(folder);
        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                invalidate(folder);
                messagingService.broadcast(new Message(MessageType.FOLDER_DELETE, folder));
            });
        }
        return result;
    }

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void addAssets(Folder folder, List<String> assetIds) {
        int result = assetDao.addToFolder(folder, assetIds);
        invalidate(folder);
        messagingService.broadcast(new Message(MessageType.FOLDER_ADD_ASSETS,
                ImmutableMap.of("added", result, "assetIds", assetIds, "folderId", folder.getId())));
    }

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void removeAssets(Folder folder, List<String> assetIds) {
        int result = assetDao.removeFromFolder(folder, assetIds);
        invalidate(folder);
        messagingService.broadcast(new Message(MessageType.FOLDER_REMOVE_ASSETS,
                ImmutableMap.of("removed", result, "assetIds", assetIds, "folderId", folder.getId())));
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
                    return folderDao.getChildrenInsecure(key);
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

            /*
             * This is a potential optimization to try out that limits the need to traverse into all
             * child folders from a root.  For example, if /exports is set with a query that searches
             * for all assets that have an export ID, then there is no need to traverse all the sub
             * folders.
             */
            if (current.isRecursive()) {
                logger.info("Folder is not recursive, skipping: {}", current);
                continue;
            }
            try {

                List<Folder> children = childCache.get(current.getId())
                        .stream()
                        .filter(f-> f.getAcl().hasAccess(SecurityUtils.getPermissionIds(), Access.Read))
                        .collect(Collectors.toList());

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
