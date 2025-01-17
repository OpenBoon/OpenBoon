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
class RestApiExceptionHandler(
    val errorAttributes: ErrorAttributes,
) {

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
        } else if (e is IncorrectResultSizeDataAccessException) {
            // We're borrowing this http status
            HttpStatus.METHOD_FAILURE
        } else if (e is DataRetrievalFailureException || e is EntityNotFoundException) {
            HttpStatus.NOT_FOUND
        } else if (e is ResponseException) {
            HttpStatus.valueOf(e.response.statusLine.statusCode)
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
        errAttrs["message"] = httpErrorMessage.getOrDefault(status, defaultErrorMessage)

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errAttrs)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestApiExceptionHandler::class.java)
        val defaultErrorMessage = "An unexpected error happened."
        val httpErrorMessage: Map<HttpStatus, String?> = mapOf(
            HttpStatus.TOO_MANY_REQUESTS to "The ES server is receiving too many requests at the moment.",
            HttpStatus.CONFLICT to "Entity conflict with current state of the target resource.",
            HttpStatus.METHOD_FAILURE to "This method has failed.",
            HttpStatus.FORBIDDEN to "The client does not have access rights to the content.",
            HttpStatus.METHOD_NOT_ALLOWED to "The request method is known by the server but is not supported by the target resource.",
            HttpStatus.BAD_REQUEST to "The server could not understand the request due to invalid syntax.",
            HttpStatus.INTERNAL_SERVER_ERROR to "The server has encountered a situation it doesn't know how to handle.",
            HttpStatus.NOT_FOUND to "The server can not find the requested resource."
        )
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
