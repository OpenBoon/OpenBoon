package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FolderDao {

    Folder get(int id);

    Folder get(int parent, String name, boolean ignorePerms);

    Folder get(Folder parent, String name);

    List<Folder> getAll(Collection<Integer> ids);

    List<Folder> getChildren(int parentId);

    List<Folder> getChildrenInsecure(int parentId);

    List<Folder> getChildren(Folder folder);

    List<Integer> getAllIds(DyHierarchy dyhi);

    boolean exists(int parentId, String name);

    int count();

    int count(DyHierarchy d);

    boolean exists(Folder parent, String name);

    Folder create(FolderSpec builder);

    Folder create(TrashedFolder spec);

    boolean update(int id, Folder folder);

    int deleteAll(DyHierarchy dyhi);

    boolean delete(Folder folder);

    int deleteAll(Collection<Integer> ids);

    boolean hasAccess(Folder folder, Access access);

    boolean setDyHierarchyRoot(Folder folder, String field);

    boolean removeDyHierarchyRoot(Folder folder);

    void setAcl(int folder, Acl acl);

    Acl getAcl(int folder);
}
