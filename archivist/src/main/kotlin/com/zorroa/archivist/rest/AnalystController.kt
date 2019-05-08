package com.zorroa.archivist.rest

import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.service.ClusterLockService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystFilter
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.domain.LockState
import io.micrometer.core.annotation.Timed
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.security.cert.X509Certificate
import java.util.*


@PreAuthorize("hasAuthority(T( com.zorroa.security.Groups).SUPERADMIN)")
@RestController
@Timed
class AnalystController @Autowired constructor(
        val analystService: AnalystService,
        val workQueue: AsyncListenableTaskExecutor,
        val clusterLockService: ClusterLockService) {

    @PostMapping(value = ["/api/v1/analysts/_search"])
    fun search(@RequestBody filter: AnalystFilter) : Any {
        return analystService.getAll(filter)
    }

    @GetMapping(value = ["/api/v1/analysts/{id}"])
    fun get( @PathVariable id: UUID) : Analyst {
        return analystService.get(id)
    }

    @PutMapping(value = ["/api/v1/analysts/{id}/_lock"])
    fun setLockState(@PathVariable id: UUID, @RequestParam(value = "state", required = true) state: String) : Any {
        val newState = LockState.valueOf(state.toLowerCase().capitalize())
        val analyst = analystService.get(id)
        return HttpUtils.updated("analyst", analyst.id, analystService.setLockState(analyst, newState))
    }

    /**
     * Initiate a processor scan.  If the processor-scan key is locked, then the "success"
     * property on the response body is set to False.  This means there is an active
     * scan already running and the request was ignored.
     */
    @PostMapping(value = ["/api/v1/analysts/_processor_scan"])
    fun processorScan(): Any {
        val locked = clusterLockService.isLocked("processor-scan")
        if (!locked) {
            workQueue.execute {
                analystService.doProcessorScan()
            }
        }
        return HttpUtils.status("processor", "scan", !locked)
    }

    /**
     * A request for this endpoint will download the ZSDK python wheel file.
     */
    @GetMapping(value = ["/download-zsdk"])
    fun downloadZsdk(requestEntity: RequestEntity<Any>): Any {
        val acceptingTrustStrategy = { chain: Array<X509Certificate>, authType: String -> true }
        val sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build()
        val csf = SSLConnectionSocketFactory(sslContext)
        val httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build()
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = httpClient
        val restTemplate = RestTemplate(requestFactory)
        val analysts = analystService.getAll(AnalystFilter(states = listOf(AnalystState.Up)))
        for (analyst in analysts) {
            val url = analyst.endpoint + "/zsdk"
            try {
                return restTemplate.exchange(url, HttpMethod.GET, requestEntity, ByteArray::class.java)
            } catch (e: Exception) {
                logger.warn("Failed to communicate with Analyst '${analyst.endpoint}", e)
            }
        }
        return ResponseEntity<Any>(HttpStatus.NOT_FOUND)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystController::class.java)
    }
}
