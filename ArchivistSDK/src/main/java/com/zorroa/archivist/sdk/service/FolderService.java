package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;

import java.util.List;
import java.util.Set;

public interface FolderService {

    Folder get(String id);

    List<Folder> getAll();
    
    List<Folder> getChildren(Folder folder);

    List<Folder> getAllDecendents(Folder folder);

    Set<String> getAllDecendentIds(List<String> folderIds);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
