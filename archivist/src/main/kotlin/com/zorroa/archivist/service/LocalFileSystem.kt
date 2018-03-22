package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.LfsRequest
import com.zorroa.archivist.domain.OnlineFileCheckRequest
import com.zorroa.archivist.domain.OnlineFileCheckResponse
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.security.getUsername
import com.zorroa.archivist.security.hasPermission
import com.zorroa.sdk.client.exception.EntityNotFoundException
import com.zorroa.sdk.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.annotation.PostConstruct


interface LocalFileSystem {
    fun listFiles(req: LfsRequest): Map<String, List<String>>

    fun exists(req: LfsRequest): Boolean

    fun suggest(req: LfsRequest): List<String>

    fun isLocalPathAllowed(path: String?): Boolean

    fun onlineFileCheck(req: OnlineFileCheckRequest): OnlineFileCheckResponse
}

@Service
class LocalFileSystemImpl @Autowired constructor(
        private val properties: ApplicationProperties
): LocalFileSystem {

    @Autowired
    private lateinit var searchService: SearchService

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
        return insecureListFiles(req)
    }

    override fun exists(req: LfsRequest): Boolean {
        permissionCheck()
        val path = FileUtils.normalize(req.path)
        return if (!isLocalPathAllowed(path)) {
            false
        } else Files.exists(Paths.get(path))
    }

    override fun onlineFileCheck(req: OnlineFileCheckRequest): OnlineFileCheckResponse  {
        val max = properties.getInt("archivist.export.maxAssetCount")
        val search = req.search
        search.fields = arrayOf("source")

        val threads =  Executors.newFixedThreadPool(4)
        val result = OnlineFileCheckResponse()
        for (doc in searchService.scanAndScroll(search, max)) {
            threads.execute({
                val path = Paths.get(doc.getAttr("source.path", String::class.java))
                if (path != null) {
                    when {
                        Files.exists(path) -> result.totalOnline.increment()
                        else -> {
                            result.totalOffline.increment()
                            result.offlineAssetIds.offer(doc.id)
                        }
                    }
                    result.total.increment()
                }
            })
        }
        threads.shutdown()
        threads.awaitTermination(1, TimeUnit.MINUTES)
        return result
    }

    override fun suggest(req: LfsRequest): List<String> {
        permissionCheck()
        val files = insecureListFiles(req)
        val result = mutableListOf<String>()

        result.addAll(files.getValue("dirs").stream().map { f -> "$f/" }.collect(Collectors.toList()))
        result.addAll(files.getValue("files"))
        result.sort()

        return result
    }

    fun insecureListFiles(req: LfsRequest): Map<String, List<String>> {
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

        result.getValue("dirs").sort()
        result.getValue("files").sort()
        return result
    }

    override fun isLocalPathAllowed(path: String?): Boolean {
        return if (pathSuggestFilter.isEmpty()) {
            false
        } else {
            var matched = false
            for (filter in pathSuggestFilter) {
                if (path!!.startsWith(filter)) {
                    matched = true
                    break
                }
            }
            matched
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
