package com.zorroa.archivist.filesystem

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.common.collect.Lists
import com.zorroa.archivist.util.StaticUtils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A very simple UUID/Object based file system.
 */
class UUIDFileSystem(root: Path) : ObjectFileSystem {

    private var baseDir: Path = root.normalize().toAbsolutePath()

    init {
        Files.createDirectories(baseDir)
    }

    override fun prepare(category: String, value: Any, type: String, variants: List<String>?): OfsFile {
        val file = get(category, value, type, variants)
        file.mkdirs()
        return file
    }

    override operator fun get(category: String, value: Any, type: String, variants: List<String>?): OfsFile {
        val uuid : UUID = when {
            value is UUID -> value
            StaticUtils.UUID_REGEXP.matches(value.toString()) -> UUID.fromString(value.toString())
            else -> nameBasedGenerator.generate(value.toString())
        }
        val sb = getParentDirectory(category, uuid)
        val name = getFilename(uuid, type, variants)
        sb.append(name)

        return OfsFile(category, name, File(sb.toString()))
    }

    override operator fun get(category: String, name: String): OfsFile {
        val matcher = REGEX_NAME.matcher(name)
        if (matcher.matches()) {
            val id = UUID.fromString(matcher.group(1))
            val ext = matcher.group(3)
            val sb = getParentDirectory(category, id)

            val variant = matcher.group(2)
            if (variant != null) {
                sb.append(getFilename(id, ext, Lists.newArrayList(variant)))
            } else {
                sb.append(getFilename(id, ext, null))
            }

            return OfsFile(category, name, File(sb.toString()))
        } else {
            throw IllegalArgumentException("Invalid object ID: $name")
        }
    }

    override operator fun get(id: String): OfsFile {
        var id = id
        id = id.replace("ofs://", "")
        val e = id.split("/".toRegex(), 2).toTypedArray()
        return get(e[0], e[1])
    }

    /**
     * Return the parent directory for the given category and unique ID.
     *
     * @param category
     * @param id
     * @return
     */
    private fun getParentDirectory(category: String, id: UUID): StringBuilder {
        val _id = id.toString()
        val sb = StringBuilder(256)
        sb.append(baseDir)
        sb.append("/")
        sb.append(category)
        sb.append("/")
        for (i in 0..DEEPNESS) {
            sb.append(_id[i])
            sb.append("/")
        }
        return sb
    }

    private fun getFilename(id: UUID, type: String, variants: List<String>?): String {
        val sb = StringBuilder(64)
        sb.append(id)
        if (variants != null && !variants.isEmpty()) {
            sb.append("_")
            sb.append(variants.joinToString("_"))
        }
        sb.append(".")
        sb.append(type)
        return sb.toString()
    }

    companion object {

        private const val DEEPNESS = 7

        private val nameBasedGenerator = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

        private val REGEX_NAME = Pattern.compile("^([a-f0-9\\-]{36})_?([_\\w]+)?\\.([\\w]+)$", Pattern.CASE_INSENSITIVE)
    }
}
