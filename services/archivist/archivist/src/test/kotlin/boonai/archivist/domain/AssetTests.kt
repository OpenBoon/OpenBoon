package boonai.archivist.domain

import boonai.archivist.util.bbox
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

class AssetTests {

    @Test
    fun addLabelsTest() {
        val ds1 = UUID.randomUUID()
        val ds2 = UUID.randomUUID()
        val asset = Asset()

        asset.addLabels(
            listOf(
                Label(ds1, "frog", bbox = bbox(0.023123, 0.12345, 0.2312323, 0.678565))
            )
        )
        asset.addLabels(
            listOf(
                Label(ds2, "dog", bbox = bbox(0.100, 0.08932, 0.7732, 0.29233))
            )
        )

        assertEquals(2, asset.getAttr("labels", Label.SET_OF)?.size)
    }
}
