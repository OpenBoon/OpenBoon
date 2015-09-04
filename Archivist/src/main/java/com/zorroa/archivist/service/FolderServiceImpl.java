package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderBuilder;
import com.zorroa.archivist.repository.FolderDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FolderServiceImpl implements  FolderService {

    @Autowired
    FolderDao folderDao;

    @Override
    public Folder get(String id) {
        return folderDao.get(id);
    }

    @Override
    public List<Folder> getAll(int userId) {
        return folderDao.getAll(userId);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return folderDao.getChildren(folder);
    }

    @Override
    public List<Folder> getAllDecendents(Folder folder) {
        ArrayList<Folder> decendents = new ArrayList<>();
        List<Folder> children = getChildren(folder);
        decendents.addAll(children);
        for (Folder child : children) {
            List<Folder> grandchildren = getAllDecendents(child);
            decendents.addAll(grandchildren);
        }
        return decendents;
    }

    @Override
    public Folder create(FolderBuilder builder) {
        return folderDao.create(builder);
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        // Convert any names in the builder to ids?
        return folderDao.update(folder, builder);
    }

    @Override
    public boolean delete(Folder folder) {
        return folderDao.delete(folder);
    }
}
