package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.sdk.services.AssetService
import com.zorroa.archivist.sdk.services.StorageService
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * Avoiding REST controller because apparently it won't let us stream
 * the file.
 */
@Controller
class FileUploadController @Autowired constructor(
        private val storageService: StorageService,
        private val assetService: AssetService
) {

    @PostMapping(value = ["/api/v1/assets/{id}/_upload/{name}"])
    @ResponseBody
    fun uploadFile(@PathVariable id: String, req: HttpServletRequest): Any {
        val upload = ServletFileUpload()
        val iter = upload.getItemIterator(req)
        while (iter.hasNext()) {
            val item = iter.next()
            if (!item.isFormField) {
                storageService.storeFile(assetService.get(UUID.fromString(id)), item.name, item.openStream())
            }
        }
        return HttpUtils.status("_upload_file", id, true)
    }

    @PostMapping(value = ["/api/v1/assets/{id}/_upload"])
    @ResponseBody
    fun upload(@PathVariable id: String, req: HttpServletRequest): Any {
        val upload = ServletFileUpload()
        val iter = upload.getItemIterator(req)
        while (iter.hasNext()) {
            val item = iter.next()
            if (!item.isFormField) {
                storageService.storeSourceFile(assetService.get(UUID.fromString(id)), item.openStream())
            }
        }

        return HttpUtils.status("_upload", id, true)
    }
}
