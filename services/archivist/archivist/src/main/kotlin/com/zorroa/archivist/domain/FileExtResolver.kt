package com.zorroa.archivist.domain

import org.apache.tika.Tika

/**
 * An enum class to define media groups.
 */
enum class FileType(val extensions: Set<String>) {
    Images(FileExtResolver.image),
    Videos(FileExtResolver.video),
    Documents(FileExtResolver.doc);

    companion object {
        fun allTypes() = listOf(Images, Videos, Documents)

        fun fromArray(types: List<String>): List<FileType> {
            val result = types.mapNotNull {
                try {
                    valueOf(it)
                } catch (e: Exception) {
                    when (it) {
                        "video" -> {
                            Videos
                        }
                        in FileExtResolver.image -> {
                            Images
                        }
                        in FileExtResolver.video -> {
                            Videos
                        }
                        in FileExtResolver.doc -> {
                            Documents
                        }
                        else -> {
                            null
                        }
                    }
                }
            }

            return if (result.isEmpty()) {
                allTypes()
            } else {
                result.toSortedSet().toList()
            }
        }

        fun fromString(types: String): List<FileType> {
            return fromArray(types.split(','))
        }
    }
}

/**
 * A singleton object for supported file types.
 */
object FileExtResolver {

    val tika = Tika()

    val image = setOf(
        "bmp",
        "cin",
        "dpx",
        "gif",
        "jpg",
        "jpeg",
        "exr",
        "png",
        "psd",
        "rla",
        "tif",
        "tiff",
        "dcm",
        "rla"
    )

    val video = setOf(
        "mov",
        "mp4",
        "mpg",
        "mpeg",
        "m4v",
        "webm",
        "ogv",
        "mxf",
        "avi"
    )

    val doc = setOf(
        "pdf",
        "doc",
        "docx",
        "ppt",
        "pptx",
        "xls",
        "xlsx",
        "vdw",
        "vsd",
        "vss",
        "vst"
    )

    val multipage = setOf(
        "exr",
        "psd",
        "tiff",
        "tif",
        "gif"
    ).plus(doc)

    val all = doc.plus(video).plus(image).toList()

    val mediaTypes = mapOf(
        "exr" to "image/x-exr",
        "dpx" to "image/x-dpx",
        "rla" to "image/x-rla",
        "cin" to "image/x-cineon"
    )

    fun isMultiPage(ext: String): Boolean {
        return ext.toLowerCase() in multipage
    }

    fun isSupported(ext: String): Boolean {
        return (ext in image || ext in video || ext in doc)
    }

    /**
     * Resolves List<FileType> into file extensions.
     */
    fun resolve(types: List<FileType>): List<String> {
        if (types.isNullOrEmpty()) {
            return all
        }

        val result = mutableListOf<String>()
        for (type in types) {
            when (type) {
                FileType.Images -> {
                    result.addAll(image)
                }
                FileType.Videos -> {
                    result.addAll(video)
                }
                FileType.Documents -> {
                    result.addAll(doc)
                }
                else -> {
                    throw IllegalArgumentException("Invalid file type: $type")
                }
            }
        }

        return result
    }

    fun getType(asset: Asset): String {
        return getType(asset.getAttr("source.extension", String::class.java) ?: "")
    }

    fun getType(ext: String): String {
        return when (ext.toLowerCase()) {
            in image -> {
                "image"
            }
            in video -> {
                "video"
            }
            in doc -> {
                "document"
            }
            else -> {
                throw IllegalArgumentException("$ext not a supported file type")
            }
        }
    }

    fun getMediaType(ext: String): String {
        val filename = if ("." in ext) {
            ext
        } else {
            "file.$ext"
        }
        return mediaTypes[ext] ?: tika.detect(filename)
    }
}
