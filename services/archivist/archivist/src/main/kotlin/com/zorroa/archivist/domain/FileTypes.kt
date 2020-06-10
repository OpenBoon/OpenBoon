package com.zorroa.archivist.domain

import org.apache.tika.Tika
import java.lang.IllegalArgumentException

/**
 * An enum class to define media groups.
 */
enum class SupportedMedia {
    Images,
    Video,
    Documents
}

/**
 * A singleton object for supported file types.
 */
object FileTypes {

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
        "tiff"
    )

    val video = setOf(
        "mov",
        "mp4",
        "mpg",
        "mpeg",
        "m4v",
        "webm",
        "ogv",
        "ogg",
        "mxf"
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

    val all = doc.plus(video).plus(image).toList()

    val mediaTypes = mapOf(
        "exr" to "image/x-exr",
        "dpx" to "image/x-dpx",
        "rla" to "image/x-rla",
        "cin" to "image/x-cineon"
    )

    fun isSupported(ext: String): Boolean {
        return (ext in image || ext in video || ext in doc)
    }

    fun resolve(types: Collection<String>?): List<String> {
        if (types.isNullOrEmpty()) {
            return all
        }

        val result = mutableListOf<String>()
        for (type in types) {
            when {
                type == "images" -> {
                    result.addAll(image)
                }
                type == "videos" -> {
                    result.addAll(video)
                }
                type == "documents" -> {
                    result.addAll(doc)
                }
                isSupported(type) -> {
                    result.add(type)
                }
                else -> {
                    throw IllegalArgumentException("Invalid file type: $type")
                }
            }
        }

        return result
    }

    fun getType(ext: String): String {
        return when (ext) {
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
