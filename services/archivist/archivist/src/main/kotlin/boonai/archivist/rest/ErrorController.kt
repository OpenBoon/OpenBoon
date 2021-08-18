package boonai.archivist.rest

import boonai.archivist.domain.ArchivistException
import boonai.archivist.domain.ArchivistSecurityException
import boonai.archivist.domain.DuplicateEntityException
import boonai.archivist.domain.EntityNotFoundException
import boonai.archivist.domain.InvalidRequestException
import boonai.archivist.security.getZmlpActorOrNull
import io.micrometer.core.annotation.Timed
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.client.ResponseException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import springfox.documentation.annotations.ApiIgnore
import java.util.UUID
import javax.servlet.http.HttpServletRequest

/**
 * The RestApiExceptionHandler converts different types of excepts into HTTP response codes.
 */
@ControllerAdvice
class RestApiExceptionHandler {

    @Autowired
    lateinit var errorAttributes: ErrorAttributes

    @Value("\${archivist.debug-mode.enabled}")
    var debug: Boolean = false

    /**
     * Do extra logging for these response statuses
     */
    val doExtraLogging =
        setOf(
            HttpStatus.UNAUTHORIZED,
            HttpStatus.BAD_REQUEST,
            HttpStatus.INTERNAL_SERVER_ERROR
        )

    val exceptionMessages = mapOf(
        ElasticsearchStatusException::class to "Elastic Service Server is Unavailable.",
        EntityNotFoundException::class to "Entity was not found or is Inaccessible.",
        ResponseException::class to "An error Occurred in the Response.",
        DataIntegrityViolationException::class to "An conflict error occurred, check if the registry already exists.",
        ArchivistSecurityException::class to "The resource cannot be reached with the used credentials.",
        HttpRequestMethodNotSupportedException::class to "HTTP method not supported",
        // known generic error
        ArchivistException::class to "An internal error occurred. We are working on it as soon as possible.",
        // unknown generic error
        Exception::class to "An internal error occurred. We are working on it as soon as possible."
    )

    @ExceptionHandler(ElasticsearchStatusException::class)
    fun elasticSearchExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: ElasticsearchStatusException
    ): ResponseEntity<ErrorResponse> {

        val status = e.message?.let {
            if (it.contains("circuit_breaking_exception"))
                HttpStatus.TOO_MANY_REQUESTS
            else null
        } ?: HttpStatus.INTERNAL_SERVER_ERROR

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[ElasticsearchStatusException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(EntityNotFoundException::class, DataRetrievalFailureException::class)
    fun dataRetrievalFailureExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.NOT_FOUND
        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[EntityNotFoundException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(ResponseException::class)
    fun responseExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: ResponseException
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.valueOf(e.response.statusLine.statusCode)

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[ResponseException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(IncorrectResultSizeDataAccessException::class)
    fun incorrectResultSizeDataAccessExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: IncorrectResultSizeDataAccessException
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.METHOD_FAILURE

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[IncorrectResultSizeDataAccessException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(DataIntegrityViolationException::class, DuplicateEntityException::class)
    fun dataIntegrityViolationExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.FORBIDDEN

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[DataIntegrityViolationException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(ArchivistSecurityException::class, AccessDeniedException::class)
    fun archivistSecurityExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.METHOD_FAILURE

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[ArchivistSecurityException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun httpRequestMethodNotSupportedExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: HttpRequestMethodNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.METHOD_NOT_ALLOWED

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[HttpRequestMethodNotSupportedException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(
        ArchivistException::class,
        ElasticsearchException::class,
        InvalidRequestException::class,
        DataAccessException::class,
        NullPointerException::class,
        IllegalArgumentException::class,
        IllegalStateException::class,
        NumberFormatException::class,
        ArrayIndexOutOfBoundsException::class
    )
    fun genericKnownErrorExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[ArchivistException::class]

        return buildResponseEntity(status, errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun genericUnknownExceptionHandler(
        wb: WebRequest,
        req: HttpServletRequest,
        e: HttpRequestMethodNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        val errorResponse = runInternalLoggerAndBuildResponse(status, req, e)
        errorResponse.message = exceptionMessages[Exception::class]

        return buildResponseEntity(status, errorResponse)
    }

    private fun buildResponseEntity(
        status: HttpStatus,
        errorResponse: ErrorResponse
    ) = ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse)

    private fun runInternalLoggerAndBuildResponse(status: HttpStatus, req: HttpServletRequest, e: Exception): ErrorResponse {

        val errorResponse = ErrorResponse(UUID.randomUUID().toString(), status.value())

        if (doExtraLogging.contains(status) || debug) {
            logger.error(
                "endpoint='{}' project='{}', errorId='{}', status='{}'",
                req.servletPath, getZmlpActorOrNull()?.projectId, errorResponse.errorId, status.value(), e
            )
        } else {
            logger.error(
                "endpoint='{}' project='{}', errorId='{}', status='{}'",
                req.servletPath, getZmlpActorOrNull()?.projectId, status.value(), errorResponse.errorId
            )
        }

        if (!debug && req.getAttribute("authType") != HttpServletRequest.CLIENT_CERT_AUTH) {
            errorResponse.genericMessage = "Please refer to errorId='${errorResponse.errorId}' for actual message"
        }

        return errorResponse
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestApiExceptionHandler::class.java)
    }
}

data class ErrorResponse(
    val errorId: String,
    val status: Int,
    var message: String? = null,
    var genericMessage: String? = null
)

@RestController
@RequestMapping("/error")
@Timed
@ApiIgnore
class CustomErrorController @Autowired constructor(private val errorAttributes: ErrorAttributes) :
    AbstractErrorController(errorAttributes), ErrorController {

    @Value("\${archivist.debug-mode.enabled}")
    var debug: Boolean = false

    override fun getErrorPath(): String {
        return "/error"
    }

    @RequestMapping
    @ResponseBody
    fun error(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val body = this.getErrorAttributes(request, debug)
        val status = this.getStatus(request)
        return ResponseEntity(body, status)
    }
}
