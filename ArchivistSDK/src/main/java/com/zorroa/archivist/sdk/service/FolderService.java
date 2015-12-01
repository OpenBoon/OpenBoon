package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FolderService {

    Folder get(String id);

    List<Folder> getAll();

    /**
     * Return all folders with the given unique IDs.
     *
     * @param ids
     * @return
     */
    List<Folder> getAll(Collection<String> ids);

    List<Folder> getChildren(Folder folder);

    /**
     * Return a Set of all descendant folders starting from the given start folders.  If
     * includeStartFolders is set to true, the starting folders are included in the result.
     *
     * @param startFolders
     * @param includeStartFolders
     * @return
     */
    Set<Folder> getAllDescendants(Collection<Folder> startFolders, boolean includeStartFolders);

    /**
     * Return a recursive list of all descendant folders from the current folder.
     *
     * @param folder
     * @return
     */
    Set<Folder> getAllDescendants(Folder folder);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
