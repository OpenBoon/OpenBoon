package com.zorroa.archivist.rest

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.TrashedFolder
import com.zorroa.archivist.domain.TrashedFolderOp
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.FolderServiceImpl
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Timed
@Api(tags = ["Trash Folder"], description = "Operations for interacting with the Trash Folder.")
class TrashFolderController @Autowired constructor(
    private val folderService: FolderService
) {

    @ApiOperation("Return a list of Trashed folders for thr current user.",
        notes = "This will only return the root level of a series of deleted folders.")
    @GetMapping(value = ["/api/v1/trash"])
    fun getAll(): List<TrashedFolder> = folderService.getTrashedFolders()

    @ApiOperation("Restore a list of trash folder IDs.")
    @PostMapping(value = ["/api/v1/trash/_restore"])
    fun restore(@ApiParam("List of Folder UUIDs to restore.") @RequestBody ids: List<UUID>): Any {
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

    @ApiOperation("Empty the Trash Folder.",
        notes = "Deletes all items in the Trash Folder unless a list of specific UUIDs is given..")
    @DeleteMapping(value = ["/api/v1/trash"])
    fun empty(@ApiParam("List Folder UUIDs to delete.") @RequestBody(required = false) ids: List<UUID>?): Any {
        return if (ids == null) {
            val result = folderService.emptyTrash()
            HttpUtils.deleted("TrashedFolder", result, !result.isEmpty())
        } else {
            val result = folderService.emptyTrash(ids)
            HttpUtils.deleted("TrashedFolder", result, !result.isEmpty())
        }
    }

    @ApiOperation("Get a count of all items in the Trash Folder.")
    @GetMapping(value = ["/api/v1/trash/_count"])
    fun count(): Any {
        return HttpUtils.count(folderService.trashCount())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FolderServiceImpl::class.java)
    }
}
