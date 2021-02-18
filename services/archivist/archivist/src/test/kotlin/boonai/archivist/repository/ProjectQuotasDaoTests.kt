package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.ProjectQuotaCounters
import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.Date
import kotlin.test.assertEquals

class ProjectQuotasDaoTests : AbstractTest() {

    @Autowired
    lateinit var projectQuotasDao: ProjectQuotasDao

    @Test
    fun testIncrementIngestTimeSeriesCounters() {

        val date = Date(1584624690000)

        val counters = ProjectQuotaCounters()
        counters.videoClipCount = 1
        counters.pageCount = 1
        counters.videoLength = 10.0
        counters.imageFileCount = 1
        counters.documentFileCount = 1
        counters.videoFileCount = 1

        counters.deletedPageCount = 1
        counters.deletedVideoClipCount = 1
        counters.deletedImageFileCount = 1
        counters.deletedDocumentFileCount = 1
        counters.deletedVideoFileCount = 1
        counters.deletedVideoLength = 10.0

        projectQuotasDao.incrementTimeSeriesCounters(date, counters)

        var row1 = jdbc.queryForMap("SELECT * FROM project_quota_time_series WHERE int_video_file_count > 0")
        assertEquals(1L, row1["int_video_file_count"])
        assertEquals(1L, row1["int_document_file_count"])
        assertEquals(1L, row1["int_image_file_count"])
        assertEquals(BigDecimal("10.00"), row1["float_video_seconds"])
        assertEquals(1L, row1["int_page_count"])
        assertEquals(1L, row1["int_video_clip_count"])

        assertEquals(BigDecimal("10.00"), row1["float_deleted_video_seconds"])
        assertEquals(1L, row1["int_deleted_video_file_count"])
        assertEquals(1L, row1["int_deleted_document_file_count"])
        assertEquals(1L, row1["int_deleted_image_file_count"])
        assertEquals(1L, row1["int_deleted_video_clip_count"])
        assertEquals(1L, row1["int_deleted_page_count"])

        projectQuotasDao.incrementTimeSeriesCounters(date, counters)
        val row2 = jdbc.queryForMap("SELECT * FROM project_quota_time_series WHERE int_video_file_count > 0")
        assertEquals(2L, row2["int_video_file_count"])
        assertEquals(2L, row2["int_document_file_count"])
        assertEquals(2L, row2["int_image_file_count"])
        assertEquals(BigDecimal("20.00"), row2["float_video_seconds"])
        assertEquals(2L, row2["int_page_count"])
        assertEquals(2L, row2["int_video_clip_count"])

        assertEquals(BigDecimal("20.00"), row2["float_deleted_video_seconds"])
        assertEquals(2L, row2["int_deleted_video_file_count"])
        assertEquals(2L, row2["int_deleted_document_file_count"])
        assertEquals(2L, row2["int_deleted_image_file_count"])
        assertEquals(2L, row2["int_deleted_video_clip_count"])
        assertEquals(2L, row2["int_deleted_page_count"])
    }

    @Test
    fun testIncrementIngestTimeScaleCountersDifferentTimes() {

        val date1 = Date(1584624690000)
        val date2 = Date(date1.time + 86400001)

        val counters = ProjectQuotaCounters()
        counters.videoClipCount = 1
        counters.pageCount = 1
        counters.videoLength = 10.0
        counters.imageFileCount = 1
        counters.documentFileCount = 1
        counters.videoFileCount = 1

        projectQuotasDao.incrementTimeSeriesCounters(date1, counters)
        projectQuotasDao.incrementTimeSeriesCounters(date2, counters)
        val row = jdbc.queryForMap(
            "SELECT SUM(int_video_file_count) AS c FROM project_quota_time_series"
        )
        assertEquals(BigDecimal(2), row["c"])

        val row2 = jdbc.queryForMap(
            "SELECT COUNT(1) AS c FROM project_quota_time_series WHERE int_video_file_count > 0"
        )
        assertEquals(BigDecimal(2), row["c"])
    }

    @Test
    fun testGetTimeSeriesCounters() {

        val date1 = Date(System.currentTimeMillis())
        val date2 = Date(date1.time + (3601000 * 2))

        val counters = ProjectQuotaCounters()
        counters.videoClipCount = 1
        counters.pageCount = 1
        counters.videoLength = 10.0
        counters.imageFileCount = 1
        counters.documentFileCount = 1
        counters.videoFileCount = 1

        projectQuotasDao.incrementTimeSeriesCounters(date1, counters)
        projectQuotasDao.incrementTimeSeriesCounters(date2, counters)
        projectQuotasDao.incrementTimeSeriesCounters(date2, counters)

        val result = projectQuotasDao.getTimeSeriesCounters(getProjectId(), date1, (date2))
        Json.prettyPrint(result)
        assertEquals(3, result.size)
        assertEquals(1, result.first().videoClipCount)
        assertEquals(2, result.last().videoFileCount)
    }
}
