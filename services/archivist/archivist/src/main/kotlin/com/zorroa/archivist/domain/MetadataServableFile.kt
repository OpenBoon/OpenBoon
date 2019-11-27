package com.zorroa.archivist.domain
//
// import com.zorroa.archivist.service.FileStat
// import com.zorroa.archivist.service.MetadataServerService
// import org.springframework.core.io.InputStreamResource
// import org.springframework.http.ResponseEntity
// import java.io.InputStream
// import java.net.URI
// import java.net.URL
// import java.nio.file.Path
// import javax.servlet.http.HttpServletResponse
//
// class MetadataServableFile(
//     private val metadataServerService: MetadataServerService,
//     val uri: URI
// ) {
//
//     fun exists(): Boolean {
//         return metadataServerService.objectExists(uri)
//     }
//
//     fun isLocal(): Boolean {
//         return metadataServerService.storedLocally
//     }
//
//     fun getSignedUrl(): URL {
//         return metadataServerService.getSignedUrl(uri)
//     }
//
//     fun getReponseEntity(): ResponseEntity<InputStreamResource> {
//         return metadataServerService.getReponseEntity(uri)
//     }
//
//     fun copyTo(response: HttpServletResponse) {
//         return metadataServerService.copyTo(uri, response)
//     }
//
//     fun getLocalFile(): Path? {
//         return metadataServerService.getLocalPath(uri)
//     }
//
//     /**
//      * Return an open InputStream for the given file.
//      */
//     fun getInputStream(): InputStream {
//         return metadataServerService.getInputStream(uri)
//     }
//
//     fun getStat(): FileStat {
//         return metadataServerService.getStat(uri)
//     }
//
//     fun delete(): Boolean {
//         return metadataServerService.delete(uri)
//     }
// }