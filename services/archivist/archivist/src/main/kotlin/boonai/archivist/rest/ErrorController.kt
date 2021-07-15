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
import org.springframework.core.annotation.AnnotationUtils
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
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

    @ExceptionHandler(Exception::class)
    fun defaultErrorHandler(wb: WebRequest, req: HttpServletRequest, e: Exception): ResponseEntity<Any> {

        val annotation = AnnotationUtils.findAnnotation(e::class.java, ResponseStatus::class.java)

        val status = if (annotation != null) {
            annotation.value
        } else if (e is ElasticsearchStatusException) {
            val msg = e.message ?: ""
            if (msg.contains("circuit_breaking_exception")) {
                HttpStatus.TOO_MANY_REQUESTS
            } else {
                HttpStatus.INTERNAL_SERVER_ERROR
            }
        } else if (e is DataRetrievalFailureException || e is EntityNotFoundException) {
            HttpStatus.NOT_FOUND
        } else if (e is ResponseException) {
            HttpStatus.valueOf(e.response.statusLine.statusCode)
        } else if (e is IncorrectResultSizeDataAccessException) {
            // We're borrowing this http status
            HttpStatus.METHOD_FAILURE
        } else if (e is DataIntegrityViolationException || e is DuplicateEntityException) {
            HttpStatus.CONFLICT
        } else if (e is ArchivistSecurityException || e is AccessDeniedException) {
            HttpStatus.FORBIDDEN
        } else if (e is HttpRequestMethodNotSupportedException ||
            e is MethodArgumentTypeMismatchException
        ) {
            HttpStatus.METHOD_NOT_ALLOWED
        } else if (e is ArchivistException ||
            e is ElasticsearchException ||
            e is InvalidRequestException ||
            e is DataAccessException ||
            e is NullPointerException ||
            e is IllegalArgumentException ||
            e is IllegalStateException ||
            e is NumberFormatException ||
            e is ArrayIndexOutOfBoundsException
        ) {
            HttpStatus.BAD_REQUEST
        } else {
            HttpStatus.INTERNAL_SERVER_ERROR
        }

        /**
         * Each error gets its own random UUID for each searching in logs.
         */
        val errorId = UUID.randomUUID().toString()

        if (doExtraLogging.contains(status) || debug) {
            logger.error(
                "endpoint='{}' project='{}', errorId='{}', status='{}'",
                req.servletPath, getZmlpActorOrNull()?.projectId, errorId, status.value(), e
            )
        } else {
            logger.error(
                "endpoint='{}' project='{}', errorId='{}', status='{}'",
                req.servletPath, getZmlpActorOrNull()?.projectId, status.value(), errorId
            )
        }

        val errAttrs = errorAttributes.getErrorAttributes(wb, debug)
        errAttrs["errorId"] = errorId
        errAttrs["status"] = status.value()

        if (!debug && req.getAttribute("authType") != HttpServletRequest.CLIENT_CERT_AUTH) {
            errAttrs["message"] = "Please refer to errorId='$errorId' for actual message"
        }

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errAttrs)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestApiExceptionHandler::class.java)
    }
}

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
