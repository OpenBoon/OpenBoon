package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.TrashedFolder;

import java.util.List;

/**
 * Created by chambers on 12/2/16.
 */
public interface TrashFolderDao {

    int create(Folder folder, String opid, boolean primary, int order);

    TrashedFolder get(int id, int userId);

    List<TrashedFolder> getAll(int userId);

    int count(int user);

    /**
     * Return all primary deleted folders for the given parent folder.
     * from the specified user.
     *
     * @param parent
     * @param userId
     * @return
     */
    List<TrashedFolder> getAll(Folder parent, int userId);

    /**
     * Return all folders deleted by the given OP id.
     * @param opId
     * @return
     */
    List<TrashedFolder> getAll(String opId);

    /**
     * Delete's all folders from a given OP.  Returns the number of
     * trash folders deleted.
     *
     * @param opid
     * @return
     */
    int removeAll(String opid);

    int removeAll(int userId);
}
