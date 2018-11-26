package com.zorroa.archivist.web.api

import com.zorroa.archivist.security.getUserOrNull
import com.zorroa.archivist.web.InvalidObjectException
import com.zorroa.common.domain.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The RestApiExceptionHandler converts different types of excepts into HTTP response codes.
 */
@ControllerAdvice
class RestApiExceptionHandler {

    @Autowired
    lateinit var errorAttributes: ErrorAttributes


    @Value("\${archivist.debug-mode.enabled}")
    var debug : Boolean = false

    @ExceptionHandler(Exception::class)
    fun defaultErrorHandler(wb: WebRequest, req: HttpServletRequest, e: Exception) : ResponseEntity<Any> {

        val annotation = AnnotationUtils.findAnnotation(e::class.java, ResponseStatus::class.java)

        val status = if (annotation != null) {
            annotation.value
        }
        else if (e is EmptyResultDataAccessException || e is EntityNotFoundException) {
           HttpStatus.NOT_FOUND
        }
        else if (e is DataIntegrityViolationException || e is DuplicateEntityException) {
            HttpStatus.CONFLICT
        }
        else if (e is ArchivistSecurityException) {
            HttpStatus.UNAUTHORIZED
        }
        else if (e is ArchivistException ||
                e is InvalidObjectException ||
                e is InvalidRequestException ||
                e is DataAccessException ||
                e is NullPointerException ||
                e is IllegalArgumentException ||
                e is IllegalStateException ||
                e is NumberFormatException ||
                e is ArrayIndexOutOfBoundsException ||
                e is MethodArgumentTypeMismatchException) {
            HttpStatus.BAD_REQUEST
        }
        else {
            HttpStatus.INTERNAL_SERVER_ERROR
        }

        /**
         * Each error gets its own random UUID for each searching in logs.
         */
        val errorId = UUID.randomUUID().toString()

        if (status == HttpStatus.INTERNAL_SERVER_ERROR || status == HttpStatus.BAD_REQUEST) {
            logger.error("endpoint='{}' user='{}', errorId='{}',",
                    req.servletPath, getUserOrNull()?.toString(), errorId, e)
        }
        else {
            logger.error("endpoint='{}' user='{}', errorId='{}',",
                    req.servletPath, getUserOrNull()?.toString(), errorId)
        }
        
        val errAttrs = errorAttributes.getErrorAttributes(wb, debug)
        errAttrs["errorId"] = errorId

        if (!debug) {
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
