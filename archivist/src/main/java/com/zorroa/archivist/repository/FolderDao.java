package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;

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

    int count();

    int count(DyHierarchy d);

    boolean exists(Folder parent, String name);

    Folder create(FolderSpec builder);

    boolean update(int id, Folder folder);

    int deleteAll(DyHierarchy dyhi);

    boolean delete(Folder folder);

    boolean hasAccess(Folder folder, Access access);

    boolean setDyHierarchyRoot(Folder folder, boolean value);

    void setAcl(Folder folder, Acl acl);

    Acl getAcl(Folder folder);
}
