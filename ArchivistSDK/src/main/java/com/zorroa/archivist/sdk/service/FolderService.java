package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;

import java.util.List;

public interface FolderService {

    Folder get(String id);

    List<Folder> getAll();

    List<Folder> getAllShared();

    List<Folder> getChildren(Folder folder);

    List<Folder> getAllDecendents(Folder folder);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
