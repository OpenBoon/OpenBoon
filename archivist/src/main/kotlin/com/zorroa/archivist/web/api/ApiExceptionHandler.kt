package com.zorroa.archivist.web.api

import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.getUserOrNull
import com.zorroa.archivist.security.getUsername
import com.zorroa.archivist.web.InvalidObjectException
import com.zorroa.common.domain.*
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.util.UrlPathHelper
import javax.servlet.http.HttpServletRequest

class Error(r: HttpServletRequest, e:Exception, clazz: Class<*>) {
    val exception: String = clazz.canonicalName
    val cause: String = e.javaClass.canonicalName
    val message : String?
    val path : String = pathHelper.getRequestUri(r)

    init {
        val sb = StringBuilder()
        sb.append(e.message)
        if (e.cause!= null) {
            sb.append(", cause: ")
            sb.append(e.cause?.message)
        }
        message = sb.toString()
    }

    companion object {
        val pathHelper = UrlPathHelper()
    }
}

@RestControllerAdvice
class ApiExceptionHandler {

    val maxStackTraceLength = 20

    fun logError(e: Exception) {

        val sb = StringBuilder(2048)
        sb.append("API Error: ")
        sb.append(e.message)
        sb.append("\n")

        if (e.cause != null) {
            sb.append("Cause: ")
            sb.append(e.cause?.message)
            sb.append("\n")
        }

        sb.append("User: ")
        try {
            sb.append(getUsername())
        }
        catch (e: Exception) {
            sb.append("Unknown Auth: {}", getUserOrNull())
        }
        sb.append("\n")

        for ((index, value) in e.stackTrace.withIndex()) {
            if (index > maxStackTraceLength) {
                break
            }
            sb.append(value.toString())
            sb.append("\n")
        }

        logger.warn(sb.toString())
    }

    @ExceptionHandler(Exception::class)
    fun  defaultErrorHandler (r: HttpServletRequest, e: Exception) : ResponseEntity<Error> {

        logError(e)

        if (e is EmptyResultDataAccessException || e is EntityNotFoundException) {
            return ResponseEntity(Error(r, e, EntityNotFoundException::class.java), HttpStatus.NOT_FOUND)
        }
        else if (e is DataIntegrityViolationException || e is DuplicateEntityException) {
            return ResponseEntity(Error(r, e, DuplicateEntityException::class.java), HttpStatus.CONFLICT)
        }
        else if (e is ArchivistWriteException) {
            return ResponseEntity(Error(r, e, ArchivistWriteException::class.java), HttpStatus.EXPECTATION_FAILED)
        }
        else if (e is InvalidObjectException ||
                e is InvalidRequestException ||
                e is DataAccessException ||
                e is NullPointerException ||
                e is IllegalArgumentException ||
                e is IllegalStateException ||
                e is NumberFormatException ||
                e is ArrayIndexOutOfBoundsException ||
                e is MethodArgumentTypeMismatchException) {
            return ResponseEntity(Error(r, e, InvalidRequestException::class.java), HttpStatus.BAD_REQUEST)
        }
        else {
            return  ResponseEntity(Error(r, e, ArchivistException::class.java), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
