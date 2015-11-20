package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.sdk.domain.DuplicateElementException;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FolderServiceImpl implements FolderService {

    @Autowired
    FolderDao folderDao;

    @Override
    public Folder get(String id) {
        return folderDao.get(id);
    }

    @Override
    public List<Folder> getAll() {
        return folderDao.getAll();
    }

    @Override
    public List<Folder> getAllShared() {
        return folderDao.getAllShared();
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

        if (!builder.getParentId().equals(Folder.ROOT_ID)) {
            Folder parent = folderDao.get(builder.getParentId());
            if (parent.getUserId() != SecurityUtils.getUser().getId()) {
                throw new AccessDeniedException("Invalid folder owner");
            }
        }

        if (folderDao.exists(builder.getParentId(), builder.getName())) {
            throw new DuplicateElementException(String.format("The folder '%s' already exists.", builder.getName()));
        }
        return folderDao.create(builder);
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        if (folder.getUserId() != SecurityUtils.getUser().getId()) {
            throw new AccessDeniedException("Invalid folder owner");
        }

        return folderDao.update(folder, builder);
    }

    @Override
    public boolean delete(Folder folder) {
        if (folder.getUserId() != SecurityUtils.getUser().getId()) {
            throw new AccessDeniedException("Invalid folder owner");
        }

        return folderDao.delete(folder);
    }
}
