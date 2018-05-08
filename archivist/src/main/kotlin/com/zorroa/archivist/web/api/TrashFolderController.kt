package com.zorroa.archivist.web.api

import com.google.common.collect.Lists
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.TrashedFolder
import com.zorroa.archivist.domain.TrashedFolderOp
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.FolderServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class TrashFolderController @Autowired constructor(
        private val folderService: FolderService
){
    /**
     * Return a list of Trashed folders for a given user.  This will only
     * return the root level of a series of deleted folders.
     *
     * @return
     */
    @GetMapping(value = ["/api/v1/trash"])
    fun getAll(): List<TrashedFolder> = folderService.getTrashedFolders()

    /**
     * Restore a list of trash folder IDs.
     *
     * @return
     */
    @PostMapping(value = ["/api/v1/trash/_restore"])
    fun restore(@RequestBody ids: List<UUID>): Any {
        val restoreOps = Lists.newArrayList<TrashedFolderOp>()
        for (id in ids) {
            try {
                val folder = folderService.getTrashedFolder(id)
                val op = folderService.restore(folder)
                restoreOps.add(op)
            } catch (e: Exception) {
                logger.warn("Failed to restore trash folder: {}", e)
            }

        }
        return HttpUtils.updated("TrashedFolder", ids, restoreOps.size > 0, restoreOps)
    }

    @DeleteMapping(value = ["/api/v1/trash"])
    fun empty(@RequestBody(required = false) ids: List<UUID>?): Any {
        return if (ids == null) {
            val result = folderService.emptyTrash()
            HttpUtils.deleted("TrashedFolder", result, !result.isEmpty())
        } else {
            val result = folderService.emptyTrash(ids)
            HttpUtils.deleted("TrashedFolder", result, !result.isEmpty())
        }
    }

    @GetMapping(value = ["/api/v1/trash/_count"])
    fun count(): Any {
        return HttpUtils.count(folderService.trashCount())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FolderServiceImpl::class.java)
    }
}
