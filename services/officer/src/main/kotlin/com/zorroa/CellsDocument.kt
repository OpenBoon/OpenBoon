package com.zorroa

import com.aspose.cells.ImageOrPrintOptions
import com.aspose.cells.ImageType
import com.aspose.cells.LoadOptions
import com.aspose.cells.MemorySetting
import com.aspose.cells.SheetRender
import com.aspose.cells.Workbook
import com.aspose.cells.Worksheet
import java.io.InputStream
import kotlin.system.measureTimeMillis

const val PAGE_LIMIT = 9
const val CELL_RANGE_MAX_COLUMNS = 10
const val CELL_RANGE_MAX_ROWS = 25

class CellsDocument(options: Options, inputStream: InputStream) : Document(options) {

    private val loadOptions = LoadOptions()

    init {
        // Set the memory preferences
        loadOptions.memorySetting = MemorySetting.MEMORY_PREFERENCE
    }

    private val workbook = Workbook(inputStream, loadOptions)

    override fun renderAllImages() {
        for (page in 0 until workbook.worksheets.count) {
            renderImage(page + 1)
        }
    }

    override fun renderAllMetadata() {
        for (page in 0 until workbook.worksheets.count) {
            renderMetadata(page + 1)
        }
    }

    override fun renderImage(page: Int) {
        val worksheet = workbook.worksheets.get(page - 1)
        val pageCount = SheetRender(worksheet, renderingOptions(false)).pageCount
        try {
            val time = measureTimeMillis {
                if (pageCount > PAGE_LIMIT) {
                    saveSheetProxyWithCellRange(worksheet, page)
                    logger.warn("Worksheet too big ($pageCount printed pages), simplified proxy generated")
                } else {
                    saveSheetProxy(worksheet, page)
                }
            }
            logImageTime(page, time)
        } catch (ex: OutOfMemoryError) {
            logger.error("Out Of Memory: Worksheet to big to render, no proxy created!")
        }
    }

    fun saveSheetProxy(worksheet: Worksheet, page: Int) {
        val sr = SheetRender(worksheet, renderingOptions(true))
        val output = ReversibleByteArrayOutputStream(8096)
        sr.toImage(0, output)
        ioHandler.writeImage(page, output)
    }

    fun saveSheetProxyWithCellRange(worksheet: Worksheet, page: Int) {
        val tmpWorkbook = Workbook()
        val tmpWorksheet = tmpWorkbook.worksheets.get(0)
        tmpWorksheet.copy(worksheet)

        val cells = tmpWorksheet.cells
        cells.deleteRows(CELL_RANGE_MAX_ROWS, cells.maxDataRow - CELL_RANGE_MAX_ROWS, false)
        cells.deleteColumns(CELL_RANGE_MAX_COLUMNS, cells.maxDataColumn - CELL_RANGE_MAX_COLUMNS, false)

        saveSheetProxy(tmpWorksheet, page)
    }

    private fun renderingOptions(singlePage: Boolean): ImageOrPrintOptions {
        val imageOrPrintOptions = ImageOrPrintOptions()
        imageOrPrintOptions.horizontalResolution = 100
        imageOrPrintOptions.verticalResolution = 100
        imageOrPrintOptions.imageType = ImageType.JPEG
        imageOrPrintOptions.onePagePerSheet = singlePage
        return imageOrPrintOptions
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val props = workbook.builtInDocumentProperties
            val metadata = mutableMapOf<String, Any?>()

            metadata["title"] = props.title
            metadata["author"] = props.author
            metadata["keywords"] = props.keywords
            metadata["description"] = props.category
            metadata["timeCreated"] = convertDate(props.createdTime?.toDate())

            metadata["pages"] = workbook.worksheets.count

            if (options.content) {
                logger.warn("Option ignored, storing worksheet content is not supported.")
            }

            val output = ReversibleByteArrayOutputStream()
            Json.mapper.writeValue(output, metadata)
            ioHandler.writeMetadata(page, output)

        }
        logMetadataTime(page, time)
    }

    override fun close() {
        try {
            workbook.dispose()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.cells.License().setLicense(licenseAsStream)
        }
    }
}
