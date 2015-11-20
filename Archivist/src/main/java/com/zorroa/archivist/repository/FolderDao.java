package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;

import java.util.List;

public interface FolderDao {

    Folder get(String id);

    List<Folder> getAll();

    List<Folder> getChildren(Folder folder);

    List<Folder> getAllShared();

    boolean exists(String parentId, String name);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
