package com.zorroa.archivist

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.util.FileUtils
import com.zorroa.archivist.util.randomString
import java.io.File

class TestAsset (
    val path: File
) : Asset(randomString(16)) {

    init {
        setAttr("source.path", path)
        setAttr("source.filename", FileUtils.filename(path))
        setAttr("source.basename", FileUtils.basename(path))
        setAttr("source.directory", FileUtils.dirname(path))
    }
}