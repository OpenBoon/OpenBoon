package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.sdk.domain.Acl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public interface FolderService {

    Folder get(int id);

    Folder get(int parent, String name);

    boolean exists(String path);

    int count();

    int count(DyHierarchy dy);

    List<Folder> getAll();

    /**
     * Return all folders with the given unique IDs.
     *
     * @param ids
     * @return
     */
    List<Folder> getAll(Collection<Integer> ids);

    List<Folder> getChildren(Folder folder);

    /**
     * Return a List of all descendant folders starting from the given start folders.  If
     * includeStartFolders is set to true, the starting folders are included in the result.
     *
     * @param startFolders
     * @param includeStartFolders
     * @return
     */
    List<Folder> getAllDescendants(Collection<Folder> startFolders, boolean includeStartFolders, boolean forSearch);

    /**
     * Return a recursive list of all descendant folders from the current folder.
     *
     * @param folder
     * @param forSearch
     * @return
     */
    List<Folder> getAllDescendants(Folder folder, boolean forSearch);

    boolean update(Folder folder, FolderSpec spec);

    boolean delete(Folder folder);

    int deleteAll(DyHierarchy dyhi);

    Folder get(String path);

    boolean setDyHierarchyRoot(Folder folder, boolean value);

    void setAcl(Folder folder, Acl acl);

    void addAssets(Folder folder, List<String> assetIds);

    void removeAssets(Folder folder, List<String> assetIds);

    /**
     * Asynchronously creata a new folder.  Return a future in case
     * you eventually need the result.
     *
     * @param spec
     * @param mightExist
     * @return
     */
    Future<Folder> submitCreate(Folder parent, FolderSpec spec, boolean mightExist);
    Future<Folder> submitCreate(FolderSpec spec, boolean mightExist);

    Folder create(FolderSpec spec);
    Folder create(FolderSpec spec, boolean mightExist);
    Folder create(Folder parent, FolderSpec spec, boolean mightExist);
}
