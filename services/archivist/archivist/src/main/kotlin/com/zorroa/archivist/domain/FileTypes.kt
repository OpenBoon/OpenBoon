package com.zorroa.archivist.domain

import java.lang.IllegalArgumentException

/**
 * A singleton object for supported file types.
 */
object FileTypes {

    val image = setOf(
        "bmp",
        "cin",
        "dds",
        "dpx",
        "gif",
        "jpg",
        "jpeg",
        "exr",
        "png",
        "psd",
        "rla",
        "tif",
        "tiff")

    val video = setOf(
        "mov",
        "mp4",
        "mpg",
        "mpeg",
        "m4v",
        "webm",
        "ogv",
        "ogg",
        "mxf")

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
        "vst")

    val all = doc.plus(video).plus(image)

    fun isSupported(ext: String): Boolean {
        return (ext in image || ext in video || ext in doc)
    }

    fun resolve(types: Collection<String>): List<String> {
        val result = mutableListOf<String>()
        for (type in types) {
            when {
                type == "IMAGES" -> {
                    result.addAll(image)
                }
                type == "VIDEOS" -> {
                    result.addAll(video)
                }
                type == "DOCUMENTS" -> {
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
}
