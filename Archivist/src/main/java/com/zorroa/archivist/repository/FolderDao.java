package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderBuilder;

import java.util.List;

public interface FolderDao {

    Folder get(String id);

    List<Folder> getAll(int userId);

    List<Folder> getChildren(Folder folder);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
