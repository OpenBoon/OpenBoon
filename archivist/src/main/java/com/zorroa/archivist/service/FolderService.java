package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    Set<Integer> getAllIds(DyHierarchy dyhi);

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

    boolean update(int id, Folder folder);

    int deleteAll(Collection<Integer> ids);

    TrashedFolderOp trash(Folder folder);

    TrashedFolderOp restore(TrashedFolder tf);

    boolean delete(Folder folder);

    int deleteAll(DyHierarchy dyhi);

    Folder get(String path);

    boolean removeDyHierarchyRoot(Folder folder, String attribute);

    boolean setDyHierarchyRoot(Folder folder, String attribute);

    void setAcl(Folder folder, Acl acl, boolean created);

    Map<String, List<Object>> addAssets(Folder folder, List<String> assetIds);

    Map<String, List<Object>> removeAssets(Folder folder, List<String> assetIds);

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

    Folder createUserFolder(String username, Permission perm);

    /**
     * Return all trashed folders for the current user.
     * @return
     */
    List<TrashedFolder> getTrashedFolders();

    /**
     * Return deleted child folders in the given folder for the current user.
     *
     * @param folder
     * @return
     */
    List<TrashedFolder> getTrashedFolders(Folder folder);

    /**
     * Get a trashed folder by its unique Id.
     *
     * @param id
     * @return
     */
    TrashedFolder getTrashedFolder(int id);

    List<Integer> emptyTrash();

    List<Integer> emptyTrash(List<Integer> ids);

    int trashCount();
}
