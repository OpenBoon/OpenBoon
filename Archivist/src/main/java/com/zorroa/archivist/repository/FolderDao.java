package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.*;

import java.util.Collection;
import java.util.List;

public interface FolderDao {

    Folder get(int id);

    Folder get(int parent, String name);

    Folder get(Folder parent, String name);

    List<Folder> getAll(Collection<Integer> ids);

    List<Folder> getChildren(int parentId);

    List<Folder> getChildrenInsecure(int parentId);

    List<Folder> getChildren(Folder folder);

    boolean exists(int parentId, String name);

    boolean exists(Folder parent, String name);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderUpdateBuilder builder);

    boolean delete(Folder folder);

    boolean hasAccess(Folder folder, Access access);

    void setAcl(Folder folder, Acl acl);

    Acl getAcl(Folder folder);
}
