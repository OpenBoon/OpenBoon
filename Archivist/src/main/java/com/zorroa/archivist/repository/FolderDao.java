package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;

import java.util.Collection;
import java.util.List;

public interface FolderDao {

    Folder get(String id);

    List<Folder> getAll(Collection<String> ids);

    List<Folder> getChildren(String parentId);

    List<Folder> getChildren(Folder folder);

    boolean exists(String parentId, String name);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
