package boonai.archivist

import boonai.archivist.domain.Asset
import boonai.archivist.util.FileUtils
import boonai.archivist.util.randomString
import java.io.File

class TestAsset(
    val path: File
) : Asset(randomString(16)) {

    init {
        setAttr("source.path", path)
        setAttr("source.filename", FileUtils.filename(path))
        setAttr("source.basename", FileUtils.basename(path))
        setAttr("source.directory", FileUtils.dirname(path))
    }
}
