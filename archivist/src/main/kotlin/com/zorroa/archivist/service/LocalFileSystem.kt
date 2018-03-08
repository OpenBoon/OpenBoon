package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.LfsRequest
import com.zorroa.archivist.security.getUsername
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.sdk.client.exception.EntityNotFoundException
import com.zorroa.sdk.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import javax.annotation.PostConstruct


interface LocalFileSystem {
    fun listFiles(req: LfsRequest): Map<String, List<String>>

    fun exists(req: LfsRequest): Boolean?

    fun suggest(req: LfsRequest): List<String>

    fun isLocalPathAllowed(path: String?): Boolean
}

@Service
class LocalFileSystemImpl @Autowired constructor(
        private val properties: ApplicationProperties
): LocalFileSystem {

    internal var pathSuggestFilter: MutableList<String> = Lists.newArrayList()

    @PostConstruct
    fun init() {
        val paths = properties.getList("archivist.lfs.paths")
        if (paths != null) {
            for (entry in paths) {
                val path = FileUtils.normalize(entry)
                pathSuggestFilter.add(path)
                logger.info("Allowing Imports from '{}'", path)
            }
        }
    }

    override fun listFiles(req: LfsRequest): Map<String, List<String>> {
        if (!hasPermission(properties.getList("archivist.lfs.permissions"))) {
            throw EntityNotFoundException("The path does not exist")
        }
        return _listFiles(req)
    }

    override fun exists(req: LfsRequest): Boolean? {
        permissionCheck()
        val path = FileUtils.normalize(req.path)
        return if (!isLocalPathAllowed(FileUtils.normalize(path))) {
            false
        } else Files.exists(Paths.get(path))
    }

    override fun suggest(req: LfsRequest): List<String> {
        permissionCheck()
        val files = _listFiles(req)
        val result = mutableListOf<String>()

        result.addAll(files["dirs"]!!.stream().map { f -> f + "/" }.collect(Collectors.toList()))
        result.addAll(files["files"]!!)
        Collections.sort(result)
        return result
    }

    fun _listFiles(req: LfsRequest): Map<String, List<String>> {
        val result = mapOf<String, MutableList<String>>(
                "dirs" to mutableListOf(),
                "files" to  mutableListOf())

        /*
         * Gotta normalize it since we allow relative paths for testing purposes.
         */
        val path = FileUtils.normalize(req.path)
        if (!isLocalPathAllowed(path)) {
            logger.warn("User {} attempted to list files in: {}",
                    getUsername(), path)
            return result
        }

        try {
            for (f in File(path).listFiles()) {
                if (f.isHidden) {
                    continue
                }
                if (req.prefix != null) {
                    if (!f.name.startsWith(req.prefix)) {
                        continue
                    }
                }

                val t = if (f.isDirectory) "dirs" else "files"

                if (t == "files" && !req.types.isEmpty()) {
                    if (!req.types.contains(FileUtils.extension(f.name))) {
                        continue
                    }
                }

                result[t]!!.add(f.name)
            }
        } catch (e: Exception) {
            return result
        }

        result["dirs"]!!.sort()
        result["files"]!!.sort()
        return result
    }

    override fun isLocalPathAllowed(path: String?): Boolean {
        if (pathSuggestFilter.isEmpty()) {
            return false
        } else {
            var matched = false
            for (filter in pathSuggestFilter) {
                if (path!!.startsWith(filter)) {
                    matched = true
                    break
                }
            }
            return matched
        }
    }

    fun permissionCheck() {
        if (!hasPermission(properties.getList("archivist.lfs.permissions"))) {
            throw EntityNotFoundException("The path does not exist")
        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger(LocalFileSystemImpl::class.java)
    }
}
