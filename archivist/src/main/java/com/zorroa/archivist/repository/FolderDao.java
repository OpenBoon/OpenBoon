package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.sdk.domain.Access;
import com.zorroa.sdk.domain.Acl;

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

    boolean update(Folder folder, FolderSpec builder);

    int deleteAll(DyHierarchy dyhi);

    boolean delete(Folder folder);

    boolean hasAccess(Folder folder, Access access);

    boolean setDyHierarchyRoot(Folder folder, boolean value);

    void setAcl(Folder folder, Acl acl);

    Acl getAcl(Folder folder);
}
