package com.zorroa.archivist.rest

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Analyst
import com.zorroa.archivist.domain.AnalystFilter
import com.zorroa.archivist.domain.AnalystState
import com.zorroa.archivist.domain.LockState
import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.security.cert.X509Certificate
import java.util.UUID

@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
@RestController
@Timed
@Api(tags = ["Analyst"], description = "Operations for managing and interacting with the Analysts.")
class AnalystController @Autowired constructor(
    val analystService: AnalystService,
    val workQueue: AsyncListenableTaskExecutor,
    val properties: ApplicationProperties
) {

    @ApiOperation("Returns a list of Analysts matching the search filter.")
    @PostMapping(value = ["/api/v1/analysts/_search"])
    fun search(@ApiParam("Search filter.") @RequestBody filter: AnalystFilter): Any {
        return analystService.getAll(filter)
    }

    @ApiOperation(
        "Searches for a single Analyst",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/analysts/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody(required = false) filter: AnalystFilter): Analyst {
        return analystService.findOne(filter)
    }

    @ApiOperation("Returns info describing an Analyst.")
    @GetMapping(value = ["/api/v1/analysts/{id}"])
    fun get(@ApiParam("UUID of the Analyst.") @PathVariable id: UUID): Analyst {
        return analystService.get(id)
    }

    @ApiOperation(
        "Sets the lock state of an Analyst.",
        notes = "Locking an Analyst prevents it from picking up any new jobs."
    )
    @PutMapping(value = ["/api/v1/analysts/{id}/_lock"])
    fun setLockState(
        @ApiParam("UUID of the Analyst.") @PathVariable id: UUID,
        @ApiParam("State to set Analyst to.", allowableValues = "locked,unlocked")
        @RequestParam(value = "state", required = true) state: String
    ): Any {
        val newState = LockState.valueOf(state.toLowerCase().capitalize())
        val analyst = analystService.get(id)
        return HttpUtils.updated("analyst", analyst.id, analystService.setLockState(analyst, newState))
    }

    @ApiOperation(
        "Initiate a custom processor scan.",
        notes = "If the processor-scan key is locked, then the \"success\" property on the response body is set to " +
            "False. This means there is an active scan already running and the request was ignored."
    )
    @PostMapping(value = ["/api/v1/analysts/_processor_scan"])
    fun processorScan(): Any {
        // TODO: utilize redis/memcached for the locking services.
        workQueue.execute {
            analystService.doProcessorScan()
        }
        return HttpUtils.status("processor", "scan", true)
    }

    @ApiOperation(
        "Download the ZSDK.",
        notes = "Downloads a universal python wheel file which can be used to install the Python SDK."
    )
    @GetMapping(value = ["/download-zsdk"])
    @PreAuthorize("permitAll()")
    fun downloadZsdk(requestEntity: RequestEntity<Any>): Any {
        if (!properties.getBoolean("archivist.zsdk-download-enabled")) {
            return ResponseEntity<Any>(HttpStatus.NOT_FOUND)
        }

        val acceptingTrustStrategy = { chain: Array<X509Certificate>, authType: String -> true }
        val sslContext = org.apache.http.ssl.SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .loadTrustMaterial(null, TrustSelfSignedStrategy())
            .build()
        val csf = SSLConnectionSocketFactory(sslContext)
        val httpClient = HttpClients.custom()
            .setSSLSocketFactory(csf)
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
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
