package com.zorroa.archivist

import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.IdGen
import com.zorroa.archivist.util.FileUtils
import java.io.File

class TestAsset (
    val path: File
) : Document(IdGen.getId(path.toString())) {

    init {
        setAttr("source.path", path)
        setAttr("source.filename", FileUtils.filename(path))
        setAttr("source.basename", FileUtils.basename(path))
        setAttr("source.directory", FileUtils.dirname(path))
    }

}