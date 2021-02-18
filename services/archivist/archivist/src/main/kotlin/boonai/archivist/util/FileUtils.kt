package boonai.archivist.util

import org.apache.tika.Tika
import org.slf4j.Logger
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator
import java.util.regex.Pattern

object FileUtils {
    private val URI_PATTERN = Pattern.compile("^\\w+://")
    private val tika = Tika()
    fun getMediaType(path: String?): String {
        return if (extension(path) == "log") {
            "text/plain"
        } else {
            tika.detect(path)
        }
    }

    fun getMediaType(path: Path): String {
        return getMediaType(path.toString())
    }

    fun getMediaType(path: File): String {
        return getMediaType(path.absolutePath)
    }

    val hostname: String
        get() {
            var hostname = System.getenv("HOSTNAME")
            var ipAddr = false
            if (hostname == null) {
                try {
                    hostname = InetAddress.getLocalHost().hostName
                } catch (ignore1: Exception) {
                    try {
                        hostname = InetAddress.getLocalHost().hostAddress
                        ipAddr = true
                    } catch (ignore2: Exception) {
                    }
                }
            }
            if (hostname == null) {
                hostname = "UnknownHost"
            }
            return if (ipAddr) {
                hostname
            } else {
                hostname.split("\\.").toTypedArray()[0]
            }
        }

    /**
     * Converts a URI or file path to a URI.
     *
     * @param path
     * @return
     */
    fun toUri(path: String): URI {
        return if (!path.startsWith("/")) {
            URI.create(path)
        } else {
            File(path).toURI()
        }
    }

    /**
     * Return true of the giving sting is a URI.
     *
     * @param path
     * @return
     */
    fun isURI(path: String?): Boolean {
        return URI_PATTERN.matcher(path).find()
    }

    /**
     * Returns an absolute normalized path for the given file path.
     *
     * @param path
     * @return
     */
    fun normalize(path: String?): String? {
        return if (path == null) null else normalize(Paths.get(path)).toString()
    }

    /**
     * Returns an absolute normalized path for the given file path.
     *
     * @param path
     * @return
     */
    fun normalize(path: File?): File? {
        return if (path == null) null else normalize(path.toPath())!!.toFile()
    }

    /**
     * Returns an absolute normalized path for the given file path.
     *
     * @param path
     * @return
     */
    fun normalize(path: Path?): Path? {
        return path?.toAbsolutePath()?.normalize()
    }

    /**
     * Return the file extension for the given path.
     *
     * @param path
     * @return
     */
    fun extension(path: String?): String {
        if (path == null) {
            return ""
        }
        try {
            return path.substring(path.lastIndexOf('.') + 1).toLowerCase()
        } catch (ignore: IndexOutOfBoundsException) { //
        }
        return ""
    }

    /**
     * Return the file extension for the given path.
     *
     * @param path
     * @return
     */
    fun extension(path: File): String {
        return extension(path.name)
    }

    /**
     * Return the file extension for the given path.
     *
     * @param path
     * @return
     */
    fun extension(path: Path): String {
        return extension(path.toString())
    }

    /**
     * Return the basename of a file file in a path.  The basename is the file name without
     * the file extension.
     *
     * In this example "file" is the base name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun basename(path: String): String {
        val filename = filename(path)
        return if (filename.contains(".")) {
            filename.substring(0, filename.lastIndexOf("."))
        } else {
            path
        }
    }

    /**
     * Return the basename of a file file in a path.  The basename is the file name without
     * the file extension.
     *
     * In this example "file" is the base name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun basename(path: File): String {
        return basename(path.name)
    }

    /**
     * Return the basename of a file file in a path.  The basename is the file name without
     * the file extension.
     *
     * In this example "file" is the base name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun basename(path: Path): String {
        return basename(path.toString())
    }

    /**
     * Return the filename of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun filename(path: String): String {
        return if (!path.contains("/")) {
            path
        } else {
            path.substring(path.lastIndexOf("/") + 1)
        }
    }

    /**
     * Return the filename of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun filename(path: File): String {
        return path.name
    }

    /**
     * Return the filename of the file in a given path.
     *
     * In this example "file.ext" is the file name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun filename(path: Path): String {
        return filename(path.toString())
    }

    /**
     * Return the directory name portion of a path.
     *
     * In this example /test/example is the directory name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun dirname(path: String): String {
        return path.substring(0, path.lastIndexOf("/"))
    }

    /**
     * Return the directory name portion of a path.
     *
     * In this example /test/example is the directory name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun dirname(path: File): String {
        return dirname(path.absolutePath)
    }

    /**
     * Return the directory name portion of a path.
     *
     * In this example /test/example is the directory name.
     * /test/example/file.ext
     *
     * @param path
     * @return
     */
    fun dirname(path: Path): String {
        return dirname(path.toAbsolutePath().toString())
    }

    /**
     * Make all the given directories in the given path. If they already exist, return.
     *
     * @param path
     */
    fun makedirs(path: String?) {
        val f = File(path)
        if (!f.exists()) {
            f.mkdirs()
        }
    }

    /**
     * Make all the given directories in the given path. If they already exist, return.
     *
     * @param path
     */
    fun makedirs(path: Path) {
        val f = path.toFile()
        if (!f.exists()) {
            f.mkdirs()
        }
    }

    private const val UNITS = "KMGTPE"
    /**
     * Converts from human readable file size to numeric bytes.
     *
     * @return byte value
     */
    fun displaySizeToByteCount(value: String): Long {
        val identifier = value.substring(value.length - 1)
        val index = UNITS.indexOf(identifier)
        var number: Long
        if (index != -1) {
            number = value.substring(0, value.length - 2).toLong()
            for (i in 0..index) {
                number = number * 1000
            }
        } else {
            number = value.toLong()
        }
        return number
    }

    /**
     * Returns a human readable display for a number of bytes.
     *
     * @param bytes
     * @return
     */
    fun byteCountToDisplaySize(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "kMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    /**
     * Quietly close the given closable.
     *
     * @param c
     */
    fun close(c: Closeable?) {
        if (c == null) {
            return
        }
        try {
            c.close()
        } catch (e: IOException) { // ignore
        }
    }

    /**
     * Look for a particular version of a file.  Expects versions
     * to end with _X.ext
     *
     * @return
     */
    fun findVersion(path: String, start: Int, max: Int): File? {
        val original = File(path)
        if (original.exists()) {
            return original
        }
        val ext = extension(path)
        for (i in start until max) {
            val newPath = path.replace(".$ext", String.format("_%d.%s", i, ext))
            val newFile = File(newPath)
            if (newFile.exists()) {
                return newFile
            }
        }
        return null
    }

    /**
     * Recursively delete a directory.
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    fun deleteRecursive(file: File, logger: Logger?): Boolean {
        return deleteRecursive(file.toPath(), logger)
    }

    /**
     * Recursively delete a directory.
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     */
    fun deleteRecursive(path: Path?, logger: Logger?): Boolean {
        try {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map { obj: Path -> obj.toFile() }
                .peek { f: File? ->
                    logger?.info("removing: {}", f)
                }
                .forEach { obj: File -> obj.delete() }
        } catch (e: IOException) {
            logger!!.warn("Failed to delete {}", path, e)
            return false
        }
        return true
    }
}
