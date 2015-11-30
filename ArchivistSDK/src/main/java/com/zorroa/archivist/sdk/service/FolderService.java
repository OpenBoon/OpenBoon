package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FolderService {

    Folder get(String id);

    List<Folder> getAll();
    
    List<Folder> getChildren(Folder folder);

    List<Folder> getAllDecendents(Folder folder);

    /**
     * Returns a Set of all downstream folder Ids starting from the given startFolderIds.  If
     * includeStartFolder is set to true, the starting folder IDs are included in the result.
     *
     * @param startFolderIds
     * @param includeStartFolders
     * @return
     */
    Set<String> getAllDescendantIds(Collection<String> startFolderIds, boolean includeStartFolders);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderBuilder builder);

    boolean delete(Folder folder);
}
